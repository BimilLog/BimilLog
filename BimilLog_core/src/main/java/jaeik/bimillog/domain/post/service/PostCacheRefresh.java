package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <h2>글 캐시 갱신 클래스</h2>
 * <p>인기글(실시간/주간/레전드) 목록 캐시의 동기 갱신을 담당합니다.</p>
 * <p>스케줄러({@link jaeik.bimillog.domain.post.scheduler.PostCacheRefreshScheduler})에서 호출됩니다.</p>
 * <p>DB 조회 후 {@link PostCacheRefreshExecutor}에 캐시 저장을 위임합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true, message = "캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final PostCacheRefreshExecutor postCacheRefreshExecutor;
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_FALLBACK_LIMIT = 5;

    /**
     * <h3>실시간 인기글 캐시 동기 갱신</h3>
     * <p>서킷 상태에 따라 Redis ZSet 또는 Caffeine에서 인기글 ID를 조회한 뒤 DB에서 상세 정보를 가져와 캐시에 저장합니다.</p>
     * <p>실패 시 최대 3회 재시도합니다 (2s → 4s 지수 백오프).</p>
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void refreshRealtime() {
        List<Long> postIds = getRealtimePostIds();
        if (postIds.isEmpty()) return;

        List<PostSimpleDetail> posts = queryPostsByType(PostCacheFlag.REALTIME, postIds);
        postCacheRefreshExecutor.cachePostsWithType(PostCacheFlag.REALTIME, posts);
    }

    /**
     * <h3>실시간 인기글 ID 조회</h3>
     * 서킷이 열리면 카페인 닫히면 레디스에서 5위까지의 인기글 ID를 조회한다.
     * @return 5위까지 글 ID
     */
    private List<Long> getRealtimePostIds() {
        List<Long> postIds;
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();

        if (state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN) {
            log.info("[SCHEDULER] REALTIME 서킷 OPEN - Caffeine 폴백으로 갱신");
            postIds = realtimeScoreFallbackStore.getTopPostIds(0, REALTIME_FALLBACK_LIMIT);
        } else {
            postIds = redisRealTimePostAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5);
        }
        return postIds;
    }


    /**
     * <h3>주간/레전드 캐시 동기 갱신</h3>
     * <p>featured_post 테이블에서 postId 조회 후 DB에서 상세 정보를 가져와 캐시에 저장합니다.</p>
     * <p>실패 시 최대 3회 재시도합니다 (2s → 4s 지수 백오프).</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND)
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void refreshFeatured(PostCacheFlag type) {
        List<PostSimpleDetail> posts = queryPostsByType(type, List.of());
        postCacheRefreshExecutor.cachePostsWithType(type, posts);
    }

    @Recover
    public void recoverRefreshRealtime(Exception e) {
        log.error("REALTIME 캐시 갱신 최종 실패 (3회 시도): {}", e.getMessage());
    }

    @Recover
    public void recoverRefreshFeatured(Exception e, PostCacheFlag type) {
        log.error("{} 캐시 갱신 최종 실패 (3회 시도): {}", type, e.getMessage());
    }

    /**
     * <h3>조회 시 HASH-ZSET 불일치 감지 → 비동기 HASH 갱신</h3>
     * <p>조회 경로에서 HASH와 ZSET의 글 ID가 불일치할 때 호출됩니다.</p>
     * <p>분산 락을 획득한 뒤 ZSET에서 인기글 ID를 조회하고 DB에서 상세 정보를 가져와 HASH를 갱신합니다.</p>
     * <p>락 획득 실패 시 다른 스레드가 이미 갱신 중이므로 스킵합니다.</p>
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshRealtimeWithLock() {
        if (!redisSimplePostAdapter.tryAcquireRealtimeRefreshLock()) {
            return;
        }

        try {
            List<Long> postIds = redisRealTimePostAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5);

            if (postIds.isEmpty()) {
                return;
            }

            List<PostSimpleDetail> posts = queryPostsByType(PostCacheFlag.REALTIME, postIds);
            postCacheRefreshExecutor.cachePostsWithType(PostCacheFlag.REALTIME, posts);
        } catch (Exception e) {
            log.warn("실시간 인기글 해시 갱신 실패: {}", e.getMessage());
        } finally {
            redisSimplePostAdapter.releaseRealtimeRefreshLock();
        }
    }

    /**
     * <h3>타입별 게시글 DB 조회</h3>
     * <p>REALTIME: 전달받은 postId 목록으로 조회</p>
     * <p>WEEKLY/LEGEND/NOTICE: featured_post 테이블에서 postId 조회 후 DB 조회</p>
     *
     * @param type       캐시 유형
     * @param allPostIds postId 목록 (REALTIME: ZSet에서 조회한 ID, 그 외: 빈 리스트)
     * @return 조회된 게시글 목록
     */
    private List<PostSimpleDetail> queryPostsByType(PostCacheFlag type, List<Long> allPostIds) {
        List<Long> postIds = (type == PostCacheFlag.REALTIME)
                ? allPostIds
                : featuredPostRepository.findPostIdsByType(type);

        if (postIds.isEmpty()) {
            return List.of();
        }

        return postIds.stream()
                .map(postId -> postQueryRepository.findPostDetail(postId, null).orElse(null))
                .filter(Objects::nonNull)
                .map(PostDetail::toSimpleDetail)
                .toList();
    }


}
