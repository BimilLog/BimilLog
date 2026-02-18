package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.PostCountCache;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
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
 * <p>ZSet(source of truth)을 먼저 조회하고, JSON LIST와 ID 순서를 비교하여 불일치 시 비동기 갱신합니다.</p>
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
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final RedisPostCounterAdapter redisPostCounterAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final RealtimePostSync realtimePostSync;
    private final PostUtil postUtil;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;

    /**
     * <h3>실시간 인기글 목록 조회</h3>
     * <p>ZSet(source of truth) 먼저 조회 → LIST 비교 → 불일치 시 비동기 갱신</p>
     * <p>서킷 OPEN → {@link #getRealtimePostsFallback(Pageable, Throwable)}에서 Caffeine 폴백</p>
     * <p>Redis 예외(서킷 CLOSED) → DB 유사 인기글 폴백</p>
     */
    @CircuitBreaker(name = REALTIME_REDIS_CIRCUIT, fallbackMethod = "getRealtimePostsFallback")
    public Page<PostSimpleDetail> getRealtimePosts(Pageable pageable) {
        // 1. ZSet top 5 조회
        List<Long> zsetTopIds = redisRealTimePostAdapter.getRangePostId();
        if (zsetTopIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2. LIST 조회 → ZSet ID 순서와 비교
        List<PostCacheEntry> entries = redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY);
        List<Long> listIds = entries.stream().map(PostCacheEntry::id).toList();
        if (!zsetTopIds.equals(listIds)) {
            realtimePostSync.asyncRebuildRealtimeCache(zsetTopIds);
        }

        // 3. 카운터 조회 후 결합
        List<Long> postIds = entries.stream().map(PostCacheEntry::id).toList();
        List<PostCountCache> counts = redisPostCounterAdapter.getCounters(postIds);
        List<PostSimpleDetail> posts = PostCacheEntry.combineAll(entries, counts);

        return postUtil.paginate(posts, pageable);
    }

    /**
     * <h3>서킷브레이커 폴백</h3>
     * <p>서킷 OPEN ({@link CallNotPermittedException}) → Caffeine 폴백</p>
     * <p>Redis 예외 (서킷 CLOSED) → DB 유사 인기글 폴백</p>
     */
    @SuppressWarnings("unused")
    private Page<PostSimpleDetail> getRealtimePostsFallback(Pageable pageable, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            return getRealtimePostsFromCaffeine(pageable);
        }

        log.warn("[REALTIME] Redis 예외, DB 폴백: {}", t.getMessage());
        return postQueryRepository.findRecentPopularPosts(pageable);
    }

    /**
     * <h3>서킷 OPEN 시 Caffeine 폴백</h3>
     * <p>Caffeine도 실패하면 DB 직접 조회</p>
     */
    private Page<PostSimpleDetail> getRealtimePostsFromCaffeine(Pageable pageable) {
        log.warn("[CIRCUIT_OPEN] 서킷 OPEN, Caffeine 폴백");
        try {
            List<Long> postIds = realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT);
            if (postIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            return postQueryRepository.findPostSimpleDetailsByIds(postIds, pageable);
        } catch (Exception e) {
            log.warn("[CAFFEINE_FALLBACK] Caffeine 폴백 실패, DB 직접 조회: {}", e.getMessage());
            return postQueryRepository.findRecentPopularPosts(pageable);
        }
    }
}
