package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static jaeik.bimillog.domain.post.repository.PostQueryType.REALTIME_FALLBACK;

import java.util.*;

/**
 * <h2>RealtimePostCacheService</h2>
 * <p>실시간 인기글 목록 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>ZSet(source of truth)에서 TOP 5 ID를 조회한 후 DB PK 조회로 게시글을 반환합니다.</p>
 * <p>서킷 OPEN 시 Caffeine → DB, Redis 예외 시 DB 폴백 경로로 전환합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimePostCacheService {
    private final PostQueryRepository postQueryRepository;
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final PostUtil postUtil;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int POST_SIZE = 5;
    private static final Pageable DEFAULT_PAGEABLE = REALTIME_FALLBACK.defaultPageable();

    /**
     * <h3>실시간 인기글 목록 조회</h3>
     * <p>ZSet 조회 → DB PK 조회 → ZSet 순서로 반환</p>
     * <p>서킷 OPEN → {@link #getRealtimePostsFallback(Throwable)}에서 Caffeine 폴백</p>
     * <p>Redis 예외(서킷 CLOSED) → DB 유사 인기글 폴백</p>
     */
    @CircuitBreaker(name = REALTIME_REDIS_CIRCUIT, fallbackMethod = "getRealtimePostsFallback")
    public Page<PostSimpleDetail> getRealtimePosts() {
        List<Long> topIds = redisPostRealTimeAdapter.getRangePostId();
        return getPostSimpleDetails(topIds);
    }

    /**
     * <h3>서킷브레이커 폴백</h3>
     * <p>서킷 OPEN ({@link CallNotPermittedException}) → Caffeine 폴백</p>
     * <p>Redis 예외 (서킷 CLOSED) → DB 유사 인기글 폴백</p>
     */
    @SuppressWarnings("unused")
    private Page<PostSimpleDetail> getRealtimePostsFallback(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            return getRealtimePostsFromCaffeine();
        }

        log.warn("[REALTIME] Redis 예외, 서킷 닫힘 DB 폴백: {}", t.getMessage());
        return postQueryRepository.selectPostSimpleDetails(PostQueryType.REALTIME_FALLBACK.condition(), DEFAULT_PAGEABLE, PostQueryType.REALTIME_FALLBACK.getOrders());
    }

    /**
     * <h3>서킷 OPEN 시 Caffeine 폴백</h3>
     * <p>Caffeine도 실패하면 DB 직접 조회</p>
     */
    private Page<PostSimpleDetail> getRealtimePostsFromCaffeine() {
        log.warn("[CIRCUIT_OPEN] 서킷 OPEN, Caffeine 폴백");
        try {
            List<Long> postIds = realtimeScoreFallbackStore.getTopPostIds(0, POST_SIZE);
            return getPostSimpleDetails(postIds);
        } catch (Exception e) {
            log.warn("[CAFFEINE_FALLBACK] Caffeine 폴백 실패, DB 직접 조회: {}", e.getMessage(), e);
            return postQueryRepository.selectPostSimpleDetails(PostQueryType.REALTIME_FALLBACK.condition(), DEFAULT_PAGEABLE, PostQueryType.REALTIME_FALLBACK.getOrders());
        }
    }

    /**
     * <h3>DB조회 로직</h3>
     */
    private Page<PostSimpleDetail> getPostSimpleDetails(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Page.empty(DEFAULT_PAGEABLE);
        }

        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            orderMap.put(postIds.get(i), i);
        }

        List<PostSimpleDetail> postList = postQueryRepository.findByIdsFetchMember(postIds).stream()
                .sorted(Comparator.comparingInt(post -> orderMap.get(post.getId())))
                .toList();
        return postUtil.paginate(postList, DEFAULT_PAGEABLE);
    }
}
