package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * <h2>PostCacheService</h2>
 * <p>캐시 조회 및 동기화 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간, 주간, 레전드 인기글 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {

    /**
     * PER의 expiry gap (초 단위)
     * <p>TTL 마지막 120초 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final int EXPIRY_GAP_SECONDS = 120;

    /**
     * 분산 락 타임아웃 (초 단위)
     * <p>캐시 갱신 작업은 10초 안에 완료되어야 합니다.</p>
     */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    private final RedisPostSaveAdapter redisPostSaveAdapter;
    private final PostQueryRepository postQueryRepository;
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
        List<Long> realtimePostIds;
        List<PostSimpleDetail> cachedList;
        // 1. score:realtime에서 상위 5개 postId 조회
        try {
            realtimePostIds = redisPostQueryAdapter.getRealtimePopularPostIds();
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
            cachedList = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);

            // 4. 비동기 갱신 트리거
            if (shouldRefresh && !cachedList.isEmpty()) {
                asyncRefreshCache(PostCacheFlag.REALTIME);
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_REALTIME_ERROR, e);
        }

        // 5. 캐시된 데이터를 Map으로 변환
        Map<Long, PostSimpleDetail> cachedMap = cachedList.stream()
                .collect(Collectors.toMap(PostSimpleDetail::getId, detail -> detail));

        List<PostSimpleDetail> resultPosts = new ArrayList<>();

        for (Long postId : realtimePostIds) {
            // 우선 캐시 맵에서 확인
            PostSimpleDetail detail = cachedMap.get(postId);
            if (detail == null) {
                // 캐시 미스 시 DB 조회
                Optional<PostDetail> postDetailOpt = postQueryRepository.findPostDetailWithCounts(postId, null);
                if (postDetailOpt.isPresent()) {
                    // DB 데이터를 DTO로 변환
                    detail = postDetailOpt.get().toSimpleDetail();

                    // 조회한 데이터를 캐시에 다시 저장 (개별 캐싱)
                    redisPostSaveAdapter.cachePostList(PostCacheFlag.REALTIME, List.of(detail));
                }
            }

            // 최종적으로 데이터가 존재하는 경우에만 리스트에 추가
            if (detail != null) {
                resultPosts.add(detail);
            }
        }

        return resultPosts;
    }

    /**
     * <h3>주간 인기 게시글 조회 (확률적 선계산 적용)</h3>
     * <p>Redis 캐시에서 주간 인기글 목록을 조회합니다.</p>
     * <p>PER 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거하며, 사용자는 기존 캐시를 즉시 반환받습니다.</p>
     *
     * @return Redis에서 조회된 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> getWeeklyPosts() {
        try {
            // Step 1: TTL 조회
            Long ttl = redisPostQueryAdapter.getPostListCacheTTL(PostCacheFlag.WEEKLY);
            List<PostSimpleDetail> currentCache = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.WEEKLY);

            // Step 2: 확률적 조기 만료 체크 (TTL - random * EXPIRY_GAP)
            if (ttl != null && ttl > 0) {
                double randomFactor = ThreadLocalRandom.current().nextDouble();
                if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) <= 0) {
                    asyncRefreshCache(PostCacheFlag.WEEKLY);
                }
            }

            return currentCache;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_WEEKLY_ERROR, e);
        }
    }

    /**
     * <h3>레전드 인기 게시글 목록 조회 (페이징, 확률적 선계산 적용)</h3>
     * <p>캐시된 레전드 게시글을 페이지네이션으로 조회합니다.</p>
     * <p>확률적 선계산 기법을 적용하여 캐시 스탬피드를 방지합니다.</p>
     * <p>TTL 마지막 2분 동안 랜덤 확률로 비동기 갱신을 트리거합니다.</p>
     *
     * @param type     조회할 인기 게시글 유형 (PostCacheFlag.LEGEND만 지원)
     * @param pageable 페이지 정보
     * @return 인기 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> getPopularPostLegend(PostCacheFlag type, Pageable pageable) {
        try {
            // Step 1: TTL 조회 및 확률적 조기 만료 체크
            Long ttl = redisPostQueryAdapter.getPostListCacheTTL(PostCacheFlag.LEGEND);
            Page<PostSimpleDetail> cachedPage = redisPostQueryAdapter.getCachedPostListPaged(pageable);

            if (ttl != null && ttl > 0) {
                double randomFactor = ThreadLocalRandom.current().nextDouble();
                if (ttl - (randomFactor * EXPIRY_GAP_SECONDS) <= 0) {
                    asyncRefreshCache(PostCacheFlag.LEGEND);
                }
            }

            return cachedPage;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_LEGEND_ERROR, e);
        }
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
        try {
            // 1. postIds:notice Set에서 실제 공지 ID 목록 조회
            List<Long> storedPostIds = redisPostQueryAdapter.getStoredPostIds(PostCacheFlag.NOTICE);

            // 2. posts:notice Hash에서 캐시된 목록 조회
            List<PostSimpleDetail> cachedList = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.NOTICE);

            // 3. 개수 비교: 저장소 ID 개수 != 캐시 목록 개수 → 캐시 미스
            if (cachedList.size() != storedPostIds.size()) {
                asyncRefreshCache(PostCacheFlag.NOTICE);
            }

            return cachedList;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.POST_REDIS_NOTICE_ERROR, e);
        }
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
            log.info("다른 스레드가 갱신 캐시 중: type={}", type);
            return;
        }

        try {
            log.info("캐시 갱신 시작: 타입={}, 스레드={}", type, Thread.currentThread().getName());
            List<Long> storedPostIds = redisPostQueryAdapter.getStoredPostIds(type); // 2티어 저장소에서 ID목록 가져옴

            // Step 2: Tier 2 PostIds로부터 복구
            if (storedPostIds.isEmpty()) {
                log.warn("캐시 갱신 실패 - 타입={}, 이유 = 2티어 저장소 비어있음", type);
                return;
            }

            // DB에서 PostDetail 조회 후 PostSimpleDetail 변환
            List<PostSimpleDetail> refreshed = storedPostIds.stream()
                    .map(postId -> postQueryRepository.findPostDetailWithCounts(postId, null).orElse(null))
                    .filter(Objects::nonNull)
                    .map(PostDetail::toSimpleDetail)
                    .toList();

            log.warn("DB로부터 응답반환 - 타입={}, 결과 사이즈={}, 스레드={}", type, refreshed.size(), Thread.currentThread().getName());

            if (refreshed.isEmpty()) {
                log.warn("캐시 갱신 실패: 타입={} DB 조회 결과가 없음", type);
                return;
            }

            // Step 3: 캐시 갱신
            redisPostSaveAdapter.cachePostList(type, refreshed);
            log.info("캐시 갱신 완료: 타입={}, count={}", type, refreshed.size());

        } catch (Exception e) {
            log.error("캐시 갱신 에러: 타입={}", type, e);
        } finally {
            // Step 4: 락 해제
            redisPostUpdateAdapter.releaseCacheRefreshLock(type);
        }
    }
}