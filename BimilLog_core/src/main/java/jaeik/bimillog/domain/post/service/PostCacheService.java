package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>PostCacheService</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>모든 타입은 Hash 캐시에서 직접 조회하며, PER(Probabilistic Early Refresh)로 TTL 15초 미만 시 선제 갱신합니다.</p>
 * <p>캐시 미스 시 SET NX 락 기반으로 비동기 캐시 갱신을 트리거합니다.</p>
 * <p>실시간 인기글은 realtimeRedis 서킷이 OPEN이면 Redis를 스킵하고 Caffeine → DB 경로를 사용합니다.</p>
 * <p>목록 캐시만 관리하며, 상세 캐시는 PostQueryService에서 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final DbFallbackGateway dbFallbackGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;

    /**
     * 실시간 인기글 목록 조회
     * <p>realtimeRedis 서킷이 OPEN이면 Redis를 스킵하고 Caffeine → DB 경로를 사용합니다.</p>
     * <p>서킷이 닫혀있으면 Hash 캐시에서 조회하고, PER로 TTL 15초 미만 시 선제 갱신합니다.</p>
     * <p>캐시 미스 시 비동기로 실시간 인기글 ZSet → DB → 캐시 갱신합니다.</p>
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        if (isRealtimeRedisCircuitOpen()) {
            return getRealtimePostsFromFallback(pageable);
        }

        try {
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME);

            if (!cachedPosts.isEmpty()) { // 캐시 미스 아님
                if (redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.REALTIME)) { // TTL로 PER 검사
                    triggerPerRealtimeCacheRefresh();
                }
                return paginate(cachedPosts, pageable);
            }

            triggerAsyncRealtimeCacheRefresh();
            log.info("[CACHE_MISS] REALTIME 캐시 미스 - DB 폴백, 비동기 갱신 트리거");
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable,
                    () -> postQueryRepository.findRecentPopularPosts(pageable));
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] REALTIME Redis 장애: {}", e.getMessage());
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable,
                    () -> postQueryRepository.findRecentPopularPosts(pageable));
        }
    }

    /**
     * 주간 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getWeeklyPosts(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, pageable);
    }

    /**
     * 전설 인기글 목록 조회
     */
    public Page<PostSimpleDetail> getPopularPostLegend(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.LEGEND, FallbackType.LEGEND, pageable);
    }

    /**
     * 공지사항 목록 조회
     */
    public Page<PostSimpleDetail> getNoticePosts(Pageable pageable) {
        return getFeaturedCachedPosts(PostCacheFlag.NOTICE, FallbackType.NOTICE, pageable);
    }

    // ========== 캐시 조회 메서드 ==========

    /**
     * <h3>주간/레전드/공지 캐시 조회</h3>
     * <p>Hash 캐시에서 직접 조회하고, PER로 TTL 15초 미만 시 선제 갱신합니다.</p>
     * <p>캐시 미스 시 비동기 갱신 트리거 + DB 폴백합니다.</p>
     *
     * @param type         캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @param fallbackType 폴백 유형
     * @param pageable     페이징 정보
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getFeaturedCachedPosts(PostCacheFlag type, FallbackType fallbackType,
                                                         Pageable pageable) {
        try {
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(type);

            if (cachedPosts.isEmpty()) {
                triggerAsyncFeaturedCacheRefresh(type);
                log.info("[CACHE_MISS] {} 캐시 미스 - DB 폴백, 비동기 갱신 트리거", type);
                return dbFallbackGateway.execute(fallbackType, pageable,
                        () -> findFeaturedPostsByType(type, pageable));
            }

            if (redisSimplePostAdapter.shouldRefreshByPer(type)) {
                triggerPerFeaturedCacheRefresh(type);
            }

            return paginate(cachedPosts, pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] {} Redis 장애: {}", type, e.getMessage());
            return dbFallbackGateway.execute(fallbackType, pageable,
                    () -> findFeaturedPostsByType(type, pageable));
        }
    }

    // ========== DB 조회 메서드 ==========

    /**
     * <h3>featured_post 테이블에서 조회 (DB 폴백용)</h3>
     * <p>featured_post 테이블에서 postId 목록을 조회 후 PostSimpleDetail로 변환합니다.</p>
     *
     * @param type     특집 유형 (WEEKLY, LEGEND, NOTICE)
     * @param pageable 페이징 정보
     * @return PostSimpleDetail 페이지
     */
    private Page<PostSimpleDetail> findFeaturedPostsByType(PostCacheFlag type, Pageable pageable) {
        List<Long> allPostIds = featuredPostRepository.findPostIdsByType(type);

        if (allPostIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allPostIds.size());

        if (start >= allPostIds.size()) {
            return new PageImpl<>(List.of(), pageable, allPostIds.size());
        }

        List<Long> pagedPostIds = allPostIds.subList(start, end);
        List<PostSimpleDetail> posts = postQueryRepository.findPostSimpleDetailsByIds(pagedPostIds);

        return new PageImpl<>(posts, pageable, allPostIds.size());
    }

    // ========== 서킷 브레이커 메서드 ==========

    /**
     * <h3>realtimeRedis 서킷 상태 확인</h3>
     * <p>서킷이 OPEN 또는 FORCED_OPEN이면 Redis가 장애 상태임을 의미합니다.</p>
     * <p>HALF_OPEN 상태에서는 Redis 접근을 허용하여 복구 여부를 확인합니다.</p>
     *
     * @return Redis 서킷이 열려있으면 true
     */
    private boolean isRealtimeRedisCircuitOpen() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();
        return state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * <h3>서킷 OPEN 시 Caffeine 폴백 경로</h3>
     * <p>Redis 서킷이 열린 상태에서 Caffeine RealtimeScoreFallbackStore의 실시간 점수 데이터를 활용합니다.</p>
     * <p>Caffeine에서 점수 내림차순으로 글 ID를 조회하고, DB에서 상세 정보를 가져옵니다.</p>
     * <p>Caffeine이 비어있으면 일반 DB 쿼리로 폴백합니다.</p>
     *
     * @param pageable 페이징 정보
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getRealtimePostsFromFallback(Pageable pageable) {
        List<Long> postIds = realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT);

        if (postIds.isEmpty()) {
            log.info("[CIRCUIT_OPEN] Caffeine 폴백 저장소 비어있음 - 일반 DB 폴백");
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable,
                    () -> postQueryRepository.findRecentPopularPosts(pageable));
        }

        log.info("[CIRCUIT_OPEN] Caffeine 폴백 사용: count={}", postIds.size());
        List<PostSimpleDetail> posts = dbFallbackGateway.executeList(
                FallbackType.REALTIME, postIds,
                () -> postQueryRepository.findPostSimpleDetailsByIds(postIds));

        return paginate(posts, pageable);
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * <h3>리스트 페이징 처리</h3>
     * <p>캐시에서 조회한 전체 리스트를 페이징합니다.</p>
     */
    private Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }

    // ========== 캐시 미스 갱신 트리거 (락 기반) ==========

    /**
     * <h3>실시간 캐시 비동기 갱신 트리거 (캐시 미스용)</h3>
     * <p>캐시 미스 시 SET NX 락을 획득한 스레드만 비동기로 실시간 인기글 ZSet → DB → 캐시 갱신합니다.</p>
     */
    private void triggerAsyncRealtimeCacheRefresh() {
        if (redisSimplePostAdapter.tryAcquireRefreshLock(PostCacheFlag.REALTIME)) {
            List<Long> postIds = redisRealTimePostAdapter.getRangePostId(
                    PostCacheFlag.REALTIME, 0, 5);

            if (!postIds.isEmpty()) {
                postCacheRefresh.asyncRefreshAllPostsWithLock(PostCacheFlag.REALTIME, postIds);
            } else {
                redisSimplePostAdapter.releaseRefreshLock(PostCacheFlag.REALTIME);
            }
        }
    }

    /**
     * <h3>주간/레전드/공지 캐시 비동기 갱신 트리거 (캐시 미스용)</h3>
     * <p>캐시 미스 시 SET NX 락을 획득한 스레드만 비동기로 featured_post → DB → 캐시 갱신합니다.</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     */
    private void triggerAsyncFeaturedCacheRefresh(PostCacheFlag type) {
        if (redisSimplePostAdapter.tryAcquireRefreshLock(type)) {
            postCacheRefresh.asyncRefreshAllPostsWithLock(type, List.of());
        }
    }

    // ========== PER 선제 갱신 트리거 (락 없음) ==========

    /**
     * <h3>실시간 PER 선제 갱신 트리거</h3>
     * <p>PER 확률 조건을 만족할 때 락 없이 비동기로 갱신합니다.</p>
     */
    private void triggerPerRealtimeCacheRefresh() {
        List<Long> postIds = redisRealTimePostAdapter.getRangePostId(
                PostCacheFlag.REALTIME, 0, 5);

        if (!postIds.isEmpty()) {
            postCacheRefresh.asyncRefreshAllPosts(PostCacheFlag.REALTIME, postIds);
        }
    }

    /**
     * <h3>주간/레전드/공지 PER 선제 갱신 트리거</h3>
     * <p>PER 확률 조건을 만족할 때 락 없이 비동기로 갱신합니다.</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     */
    private void triggerPerFeaturedCacheRefresh(PostCacheFlag type) {
        postCacheRefresh.asyncRefreshAllPosts(type, List.of());
    }
}
