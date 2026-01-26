package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <h2>글 캐시 갱신 클래스</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시의 비동기 갱신을 담당합니다.</p>
 * <p>PER(Probabilistic Early Refresh) 기반으로 목록 API에서 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, message = "캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final DbFallbackGateway dbFallbackGateway;

    /**
     * <h3>타입별 전체 Hash 캐시 비동기 갱신 (락 해제 포함)</h3>
     * <p>캐시 미스 시 SET NX 락을 획득한 스레드가 호출합니다.</p>
     * <p>갱신 완료 후 락을 해제하여 다음 캐시 미스 시 다시 갱신할 수 있도록 합니다.</p>
     *
     * @param type       캐시 유형
     * @param allPostIds 갱신할 postId 목록
     */
    @Async("cacheRefreshExecutor")
    @Retryable(
            retryFor = RedisConnectionFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 1)
    )
    public void asyncRefreshAllPostsWithLock(PostCacheFlag type, List<Long> allPostIds) {
        try {
            refreshInternal(type, allPostIds);
        } finally {
            redisSimplePostAdapter.releaseRefreshLock(type);
        }
    }

    @Recover
    public void recoverRefreshAllPostsWithLock(RedisConnectionFailureException e, PostCacheFlag type, List<Long> allPostIds) {
        log.debug("목록 캐시 갱신 스킵 (락 해제): type={}", type);
        redisSimplePostAdapter.releaseRefreshLock(type);
    }

    /**
     * <h3>타입별 전체 Hash 캐시 비동기 갱신 (PER용, 락 없음)</h3>
     * <p>PER 선제 갱신 시 호출됩니다. 락 없이 비동기로 갱신합니다.</p>
     * <p>PER은 확률적 메커니즘이므로 자체적으로 동시 갱신이 제한되며, 스레드풀이 자연스러운 스로틀링 역할을 합니다.</p>
     * <p>실패 시 재시도하지 않습니다. 캐시 만료 시 캐시 미스 경로에서 락 기반 갱신이 실행됩니다.</p>
     *
     * @param type       캐시 유형
     * @param allPostIds 갱신할 postId 목록 (REALTIME: ZSet에서 조회한 ID, 그 외: 빈 리스트)
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshAllPosts(PostCacheFlag type, List<Long> allPostIds) {
        try {
            refreshInternal(type, allPostIds);
        } catch (Exception e) {
            log.debug("[PER] 목록 캐시 갱신 실패: type={}, error={}", type, e.getMessage());
        }
    }

    /**
     * <h3>캐시 갱신 내부 로직</h3>
     * <p>REALTIME: 전달받은 postId 목록으로 갱신</p>
     * <p>WEEKLY/LEGEND/NOTICE: featured_post 테이블에서 postId 조회 후 갱신</p>
     */
    private void refreshInternal(PostCacheFlag type, List<Long> allPostIds) {
        // REALTIME이 아닌 경우 featured_post 테이블에서 postId 조회
        List<Long> postIds = (type == PostCacheFlag.REALTIME)
                ? allPostIds
                : featuredPostRepository.findPostIdsByType(type);

        if (postIds.isEmpty()) {
            return;
        }

        List<PostSimpleDetail> refreshed = postIds.stream()
                .map(postId -> dbFallbackGateway.executeDetail(
                        FallbackType.POPULAR_DETAIL,
                        postId,
                        () -> postQueryRepository.findPostDetail(postId, null)
                ).orElse(null))
                .filter(Objects::nonNull)
                .map(PostDetail::toSimpleDetail)
                .toList();

        if (refreshed.isEmpty()) {
            return;
        }

        redisSimplePostAdapter.cachePosts(type, refreshed);
    }
}
