package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>RealtimePostCacheService</h2>
 * <p>실시간 인기글 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>Hash 캐시에서 직접 조회하며, PER(Probabilistic Early Refresh)로 TTL 15초 미만 시 선제 갱신합니다.</p>
 * <p>캐시 미스 시 동기적으로 ZSet을 확인하고, 데이터가 있으면 DB 조회 후 비동기 갱신을 트리거합니다.</p>
 * <p>realtimeRedis 서킷이 OPEN이면 Redis를 스킵하고 Caffeine -> DB 경로를 사용합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimePostCacheService {
    private final PostQueryRepository postQueryRepository;
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
     * <p>realtimeRedis 서킷이 OPEN이면 Redis를 스킵하고 Caffeine -> DB 경로를 사용합니다.</p>
     * <p>서킷이 닫혀있으면 Hash 캐시에서 조회하고, PER로 TTL 15초 미만 시 선제 갱신합니다.</p>
     * <p>캐시 미스 시 동기적으로 ZSet을 확인하고 DB 조회 후 비동기 갱신을 트리거합니다.</p>
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        if (isRealtimeRedisCircuitOpen()) {
            try {
                return getRealtimePostsFromFallback(pageable);
            } catch (Exception e) {
                log.warn("[CAFFEINE_FALLBACK] Caffeine 폴백 실패: {}", e.getMessage());
                return dbFallbackGateway.execute(FallbackType.REALTIME, pageable,
                        () -> postQueryRepository.findRecentPopularPosts(pageable));
            }
        }

        try {
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME);

            if (!cachedPosts.isEmpty()) {
                if (redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.REALTIME)) {
                    triggerPerRealtimeCacheRefresh();
                }
                return paginate(cachedPosts, pageable);
            }

            // 캐시 미스: ZSet에서 실시간 인기글 ID 동기 조회
            List<Long> postIds = redisRealTimePostAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5);

            if (postIds.isEmpty()) {
                log.info("[CACHE_MISS] REALTIME 캐시 미스 - ZSet 비어있음, 빈 페이지 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }

            log.info("[CACHE_MISS] REALTIME 캐시 미스 - ZSet -> DB 폴백, 비동기 갱신 트리거");
            List<PostSimpleDetail> posts = dbFallbackGateway.executeList(
                    FallbackType.REALTIME, postIds,
                    () -> postQueryRepository.findPostSimpleDetailsByIds(postIds));
            postCacheRefresh.asyncRefreshRealtimeWithLock(posts);
            return paginate(posts, pageable);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] REALTIME Redis 장애: {}", e.getMessage());
            return dbFallbackGateway.execute(FallbackType.REALTIME, pageable,
                    () -> postQueryRepository.findRecentPopularPosts(pageable));
        }
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
     * <p>Caffeine이 비어있으면 빈 페이지를 반환합니다.</p>
     *
     * @param pageable 페이징 정보
     * @return 페이징된 게시글 목록
     */
    private Page<PostSimpleDetail> getRealtimePostsFromFallback(Pageable pageable) {
        List<Long> postIds = realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT);

        if (postIds.isEmpty()) {
            log.info("[CIRCUIT_OPEN] Caffeine 폴백 비어있음 - 빈 페이지 반환");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        log.info("[CIRCUIT_OPEN] Caffeine 폴백 사용: count={}", postIds.size());
        List<PostSimpleDetail> posts = dbFallbackGateway.executeList(
                FallbackType.REALTIME, postIds,
                () -> postQueryRepository.findPostSimpleDetailsByIds(postIds));

        return paginate(posts, pageable);
    }

    // ========== 유틸리티 메서드 ==========

    private Page<PostSimpleDetail> paginate(List<PostSimpleDetail> posts, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), posts.size());

        if (start >= posts.size()) {
            return new PageImpl<>(List.of(), pageable, posts.size());
        }

        return new PageImpl<>(posts.subList(start, end), pageable, posts.size());
    }

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
}
