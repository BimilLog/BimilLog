package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>RealtimePostCacheService</h2>
 * <p>실시간 인기글 목록 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>JSON LIST에서 직접 조회하며, 캐시 미스 시 ZSet → DB → replaceAll로 재구축합니다.</p>
 * <p>realtimeRedis 서킷브레이커가 적용되어 Redis 장애 시 Caffeine → DB 폴백 경로로 전환합니다.</p>
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
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final PostUtil postUtil;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;

    /**
     * 실시간 인기글 목록 조회
     * <p>JSON LIST에서 직접 조회합니다.</p>
     * <p>캐시 미스 시 ZSet에서 top ID → DB 조회 → JSON LIST 재구축 후 반환합니다.</p>
     * <p>Redis 장애 시 서킷브레이커가 {@link #getRealtimePostsFallback}을 호출합니다.</p>
     */
    @CircuitBreaker(name = REALTIME_REDIS_CIRCUIT, fallbackMethod = "getRealtimePostsFallback")
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        List<PostSimpleDetail> posts = redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY);

        if (!posts.isEmpty()) {
            CacheMetricsLogger.hit(log, "realtime", "json-list", posts.size());
            return postUtil.paginate(posts, pageable);
        }

        // 캐시 미스: ZSet에서 top ID → DB 조회 → JSON LIST 재구축
        CacheMetricsLogger.miss(log, "realtime", "json-list", "empty");
        List<Long> orderedIds = redisRealTimePostAdapter.getRangePostId();
        if (orderedIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<PostSimpleDetail> dbPosts = postQueryRepository
                .findPostSimpleDetailsByIds(orderedIds, PageRequest.of(0, REALTIME_FALLBACK_LIMIT))
                .getContent();

        if (!dbPosts.isEmpty()) {
            redisPostJsonListAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, dbPosts, RedisKey.DEFAULT_CACHE_TTL);
        }

        return postUtil.paginate(dbPosts, pageable);
    }

    /**
     * <h3>서킷 OPEN 또는 Redis 예외 시 폴백</h3>
     * <p>Caffeine RealtimeScoreFallbackStore에서 점수 내림차순으로 글 ID를 조회하고, DB에서 상세 정보를 가져옵니다.</p>
     * <p>Caffeine 폴백도 실패하면 DB에서 직접 조회합니다.</p>
     */
    private Page<PostSimpleDetail> getRealtimePostsFallback(Pageable pageable, Throwable t) {
        log.warn("[CIRCUIT_FALLBACK] 실시간 인기글 Redis 장애: {}", t.getMessage());
        try {
            return getRealtimePostsFromCaffeine(pageable);
        } catch (Exception e) {
            log.warn("[CAFFEINE_FALLBACK] Caffeine 폴백 실패: {}", e.getMessage());
            return postQueryRepository.findRecentPopularPosts(pageable);
        }
    }

    /**
     * <h3>Caffeine 폴백 경로</h3>
     */
    private Page<PostSimpleDetail> getRealtimePostsFromCaffeine(Pageable pageable) {
        List<Long> postIds = realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT);

        if (postIds.isEmpty()) {
            log.info("[CIRCUIT_OPEN] Caffeine 폴백 비어있음 - 빈 페이지 반환");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        log.info("[CIRCUIT_OPEN] Caffeine 폴백 데이터 있음 (count={}) - DB 배치 조회", postIds.size());
        return postQueryRepository.findPostSimpleDetailsByIds(postIds, pageable);
    }
}
