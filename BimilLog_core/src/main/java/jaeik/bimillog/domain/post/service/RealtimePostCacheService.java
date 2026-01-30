package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>RealtimePostCacheService</h2>
 * <p>실시간 인기글 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>Hash 캐시에서 직접 조회하며, 캐시 미스 시 빈 페이지를 즉시 반환합니다.</p>
 * <p>캐시 갱신은 {@link jaeik.bimillog.domain.post.scheduler.PostCacheRefreshScheduler}가 담당합니다.</p>
 * <p>realtimeRedis 서킷이 OPEN이면 Redis를 스킵하고 빈 페이지를 반환합니다.</p>
 * <p>예외 발생 시 DB에서 직접 조회합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimePostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final PostCacheRefresh postCacheRefresh;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;

    /**
     * 실시간 인기글 목록 조회
     * <p>realtimeRedis 서킷이 OPEN이면 Redis를 스킵하고 빈 페이지를 반환합니다.</p>
     * <p>서킷이 닫혀있으면 Hash 캐시에서 조회하고, ZSET과 ID를 비교합니다.</p>
     * <p>HASH-ZSET ID가 불일치하면 비동기로 락을 획득하여 HASH를 갱신합니다.</p>
     */
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
//        if (isRealtimeRedisCircuitOpen()) {
//            try {
//                return getRealtimePostsFromFallback(pageable);
//            } catch (Exception e) {
//                log.warn("[CAFFEINE_FALLBACK] Caffeine 폴백 실패: {}", e.getMessage());
//                return postQueryRepository.findRecentPopularPosts(pageable);
//            }
//        }

        try {
            List<PostSimpleDetail> cachedPosts = redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME);

            if (!cachedPosts.isEmpty()) {
                CacheMetricsLogger.hit(log, "realtime", "simple", cachedPosts.size());
                compareAndTriggerRefreshIfNeeded(cachedPosts);
                return paginate(cachedPosts, pageable);
            }

            CacheMetricsLogger.miss(log, "realtime", "simple", "empty");
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] REALTIME Redis 장애: {}", e.getMessage());
            return postQueryRepository.findRecentPopularPosts(pageable);
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

        log.info("[CIRCUIT_OPEN] Caffeine 폴백 데이터 있음 (count={}) - 빈 페이지 반환", postIds.size());
        return new PageImpl<>(List.of(), pageable, 0);
    }

    // ========== HASH-ZSET 비교 메서드 ==========

    /**
     * <h3>HASH-ZSET ID 비교 후 불일치 시 비동기 갱신 트리거</h3>
     * <p>HASH에 저장된 글 ID와 ZSET 상위 글 ID를 비교합니다.</p>
     * 비동기로 락 획득 → HASH 갱신을 수행합니다.</p>
     * <p>ZSET 조회 실패 시 비교를 스킵합니다 (기존 HASH 데이터를 그대로 반환).</p>
     *
     * @param cachedPosts HASH에서 조회한 게시글 목록 (빈 리스트 가능)
     */
    private void compareAndTriggerRefreshIfNeeded(List<PostSimpleDetail> cachedPosts) {
        try {
            Set<Long> hashPostIds = cachedPosts.stream()
                    .map(PostSimpleDetail::getId)
                    .collect(Collectors.toSet());

            List<Long> zsetPostIds = redisRealTimePostAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5);
            if (zsetPostIds.isEmpty()) {
                return;
            }
            Set<Long> zsetPostIdSet = new HashSet<>(zsetPostIds);

            if (!hashPostIds.equals(zsetPostIdSet)) {

                postCacheRefresh.asyncRefreshRealtimeWithLock(zsetPostIds);
            }
        } catch (Exception e) {
            log.debug("[COMPARE_SKIP] HASH-ZSET 비교 실패, 무시: {}", e.getMessage());
        }
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

}
