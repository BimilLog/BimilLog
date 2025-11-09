package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.out.PostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostSaveAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * <h2>PostCacheService</h2>
 * <p>게시글 캐시 관리 관련 UseCase 인터페이스의 구현체로서 캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간, 주간, 레전드 인기글 조회</p>
 * <p>공지사항 조회 및 상태 변경에 따른 Redis 캐시 동기화와 데이터 무결성 보장을 위한 비즈니스 규칙을 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {

    // ===================== 상수 =====================

    /**
     * 확률적 선계산(Probabilistic Early Expiration) 기법의 expiry gap (초 단위)
     * <p>TTL 마지막 120초(2분) 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final int EXPIRY_GAP_SECONDS = 120;

    /**
     * 분산 락 타임아웃 (초 단위)
     * <p>캐시 갱신 작업은 10초 안에 완료되어야 합니다.</p>
     */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    // ===================== 의존성 =====================

    private final RedisPostSaveAdapter redisPostSaveAdapter;
    private final PostQueryAdapter postQueryAdapter;
    private final RedisPostQueryAdapter redisPostQueryAdapter;
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;

    /**
     * <h3>실시간 인기 게시글 조회 (확률적 선계산 적용)</h3>
     * <p>Redis Sorted Set에서 postId 목록을 조회하고 posts:realtime Hash에서 상세 정보를 획득합니다.</p>
     * <p>확률적 선계산 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거합니다.</p>
     *
     * @return Redis에서 조회된 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getRealtimePosts() {
        // 1. score:realtime에서 상위 5개 postId 조회
        List<Long> realtimePostIds = redisPostQueryAdapter.getRealtimePopularPostIds();
        if (realtimePostIds.isEmpty()) {
            return List.of();
        }

        // 2. TTL 조회 및 확률적 조기 만료 체크
        Long ttl = redisPostQueryAdapter.getPostListCacheTTL(PostCacheFlag.REALTIME);
        boolean shouldRefresh = false;

        if (ttl != null && ttl > 0) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) <= 0) {
                shouldRefresh = true;
            }
        }

        // 3. posts:realtime Hash에서 PostSimpleDetail 조회
        List<PostSimpleDetail> cachedList = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);

        // 4. 비동기 갱신 트리거 (TTL 임계값 이하이고 캐시가 있을 때)
        if (shouldRefresh && !cachedList.isEmpty()) {
            asyncRefreshCache(PostCacheFlag.REALTIME);
        }

        // 5. 캐시된 데이터를 Map으로 변환 (빠른 조회)
        Map<Long, PostSimpleDetail> cachedMap = cachedList.stream()
                .collect(Collectors.toMap(PostSimpleDetail::getId, detail -> detail));

        // 6. postId 순서대로 조회하며 캐시 미스 시 DB 조회 후 추가
        return realtimePostIds.stream()
                .map(postId -> {
                    PostSimpleDetail cached = cachedMap.get(postId);
                    if (cached != null) {
                        return cached;
                    }

                    // 캐시 미스: DB 조회 후 posts:realtime에 추가
                    PostDetail postDetail = postQueryAdapter.findPostDetailWithCounts(postId, null).orElse(null);
                    if (postDetail == null) {
                        return null;
                    }

                    PostSimpleDetail simpleDetail = postDetail.toSimpleDetail();
                    redisPostSaveAdapter.cachePostList(PostCacheFlag.REALTIME, List.of(simpleDetail));
                    return simpleDetail;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * <h3>주간 인기 게시글 조회 (확률적 선계산 적용)</h3>
     * <p>Redis 캐시에서 주간 인기글 목록을 조회합니다.</p>
     * <p>확률적 선계산(Probabilistic Early Expiration) 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거하며, 사용자는 기존 캐시를 즉시 반환받습니다.</p>
     *
     * @return Redis에서 조회된 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getWeeklyPosts() {
        // Step 1: TTL 조회
        Long ttl = redisPostQueryAdapter.getPostListCacheTTL(PostCacheFlag.WEEKLY);

        // Step 2: 확률적 조기 만료 체크 (TTL - random * EXPIRY_GAP)
        if (ttl != null && ttl > 0) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) > 0) {
                // TTL 충분: 캐시 반환
                return redisPostQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);
            }
        }

        // Step 3: TTL 임계값 이하 or 캐시 없음
        List<PostSimpleDetail> currentCache = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);

        if (!currentCache.isEmpty()) {
            // 기존 캐시 있음: 비동기 갱신 트리거 + 즉시 반환
            asyncRefreshCache(PostCacheFlag.WEEKLY);
            return currentCache;
        } else {
            // 캐시 완전 없음 (cold start): 동기 복구
            List<PostSimpleDetail> recovered = recoverFromStoredPostIds(PostCacheFlag.WEEKLY);
            if (!recovered.isEmpty()) {
                redisPostSaveAdapter.cachePostList(PostCacheFlag.WEEKLY, recovered);
            }
            return recovered;
        }
    }

    /**
     * <h3>레전드 인기 게시글 목록 조회 (페이징, 확률적 선계산 적용)</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다.</p>
     * <p>확률적 선계산 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거합니다.</p>
     *
     * @param type 조회할 인기 게시글 유형 (PostCacheFlag.LEGEND만 지원)
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     * @throws PostCustomException 유효하지 않은 캐시 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> getPopularPostLegend(PostCacheFlag type, Pageable pageable) {
        // Step 1: TTL 조회 및 확률적 조기 만료 체크
        Long ttl = redisPostQueryAdapter.getPostListCacheTTL(PostCacheFlag.LEGEND);
        boolean shouldRefresh = false;

        if (ttl != null && ttl > 0) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) <= 0) {
                shouldRefresh = true;
            }
        }

        // Step 2: 캐시된 페이지 조회
        Page<PostSimpleDetail> cachedPage = redisPostQueryAdapter.getCachedPostListPaged(pageable);

        // Step 3: 비동기 갱신 트리거 (TTL 임계값 이하이고 캐시가 있을 때)
        if (shouldRefresh && !cachedPage.isEmpty()) {
            asyncRefreshCache(PostCacheFlag.LEGEND);
        }

        // Step 4: 캐시 미스 시 동기 복구
        if (cachedPage.isEmpty()) {
            List<PostSimpleDetail> recovered = recoverFromStoredPostIds(PostCacheFlag.LEGEND);
            if (!recovered.isEmpty()) {
                redisPostSaveAdapter.cachePostList(PostCacheFlag.LEGEND, recovered);
                cachedPage = redisPostQueryAdapter.getCachedPostListPaged(pageable);
            }
        }

        return cachedPage;
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>Redis에 캐시된 공지사항 목록을 조회합니다.</p>
     * <p>postIds 저장소 ID 개수와 캐시 목록 개수를 비교하여 정합성을 검증합니다.</p>
     * <p>개수 불일치 시 캐시 미스로 판단하고 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 반환합니다.</p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getNoticePosts() {
        // 1. postIds:notice Set에서 실제 공지 ID 목록 조회
        List<Long> storedPostIds = redisPostQueryAdapter.getStoredPostIds(PostCacheFlag.NOTICE);

        // 2. posts:notice Hash에서 캐시된 목록 조회
        List<PostSimpleDetail> cachedList = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.NOTICE);

        // 3. 개수 비교: 저장소 ID 개수 != 캐시 목록 개수 → 캐시 미스
        if (cachedList.size() != storedPostIds.size()) {
            return recoverFromStoredPostIds(PostCacheFlag.NOTICE);
        }

        return cachedList;
    }

    /**
     * <h3>postIds 저장소에서 캐시 복구</h3>
     * <p>목록 캐시 TTL 만료 시 postIds 저장소에서 ID 목록을 가져와 DB 조회 후 목록을 재구성합니다.</p>
     *
     * @param type 복구할 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 복구된 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<PostSimpleDetail> recoverFromStoredPostIds(PostCacheFlag type) {
        log.warn("[CACHE_RECOVERY] START - type={}, thread={}", type, Thread.currentThread().getName());

        List<Long> storedPostIds = redisPostQueryAdapter.getStoredPostIds(type);
        if (storedPostIds.isEmpty()) {
            log.warn("[CACHE_RECOVERY] FAIL - type={}, reason=postIds_empty", type);
            return List.of();
        }

        log.warn("[CACHE_RECOVERY] DB_QUERY - type={}, postIds={}, count={}", type, storedPostIds, storedPostIds.size());

        // DB에서 PostDetail 조회 후 PostSimpleDetail 변환
        List<PostSimpleDetail> result = storedPostIds.stream()
                .map(postId -> postQueryAdapter.findPostDetailWithCounts(postId, null).orElse(null))
                .filter(Objects::nonNull)
                .map(PostDetail::toSimpleDetail)
                .toList();

        log.warn("[CACHE_RECOVERY] END - type={}, recovered={}, thread={}", type, result.size(), Thread.currentThread().getName());
        return result;
    }

    /**
     * <h3>비동기 캐시 갱신 (분산 락 기반)</h3>
     * <p>확률적 선계산 기법에서 TTL 임계값 이하일 때 호출되는 비동기 캐시 갱신 메서드입니다.</p>
     * <p>분산 락을 사용하여 여러 요청이 동시에 갱신을 시도해도 1회만 실행되도록 보장합니다.</p>
     * <p>백그라운드에서 실행되므로 사용자 요청은 블로킹되지 않습니다.</p>
     *
     * @param type 갱신할 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshCache(PostCacheFlag type) {
        // Step 1: 분산 락 획득 시도 (10초 타임아웃)
        Boolean acquired = redisPostUpdateAdapter.acquireCacheRefreshLock(type, LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(acquired)) {
            log.debug("[CACHE_REFRESH] 다른 스레드가 갱신 중: type={}", type);
            return;  // 다른 스레드가 이미 갱신 중
        }

        try {
            log.info("[CACHE_REFRESH] 시작: type={}, thread={}", type, Thread.currentThread().getName());

            // Step 2: Tier 2 PostIds로부터 복구
            List<PostSimpleDetail> refreshed = recoverFromStoredPostIds(type);

            if (!refreshed.isEmpty()) {
                // Step 3: 캐시 갱신
                redisPostSaveAdapter.cachePostList(type, refreshed);
                log.info("[CACHE_REFRESH] 완료: type={}, count={}", type, refreshed.size());
            } else {
                log.warn("[CACHE_REFRESH] 복구 실패: type={} (Tier 2 PostIds 없음)", type);
            }

        } catch (Exception e) {
            log.error("[CACHE_REFRESH] 에러: type={}", type, e);
        } finally {
            // Step 4: 락 해제
            redisPostUpdateAdapter.releaseCacheRefreshLock(type);
        }
    }
}