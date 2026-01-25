package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
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
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final DbFallbackGateway dbFallbackGateway;

    /**
     * <h3>타입별 전체 Hash 캐시 비동기 갱신 (PER 기반)</h3>
     * <p>TTL 만료 임박 시 확률적으로 호출됩니다.</p>
     * <p>PER은 이미 확률적으로 분산되어 있어 락을 사용하지 않습니다.</p>
     *
     * @param type 캐시 유형
     */
    @Async("cacheRefreshExecutor")
    @Retryable(
            retryFor = RedisConnectionFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 1)
    )
    public void asyncRefreshAllPosts(PostCacheFlag type, List<Long> allPostIds) {
        refreshInternal(type, allPostIds);
    }

    @Recover
    public void recoverRefreshAllPosts(RedisConnectionFailureException e, PostCacheFlag type, List<Long> allPostIds) {
        log.debug("목록 캐시 갱신 스킵: type={}", type);
    }

    /**
     * <h3>타입별 전체 Hash 캐시 비동기 갱신 (락 해제 포함)</h3>
     * <p>ID 불일치 시 SET NX 락을 획득한 스레드가 호출합니다.</p>
     * <p>갱신 완료 후 락을 해제하여 다음 불일치 시 다시 갱신할 수 있도록 합니다.</p>
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
            redisSimplePostAdapter.releaseRefreshLock();
        }
    }

    @Recover
    public void recoverRefreshAllPostsWithLock(RedisConnectionFailureException e, PostCacheFlag type, List<Long> allPostIds) {
        log.debug("목록 캐시 갱신 스킵 (락 해제): type={}", type);
        redisSimplePostAdapter.releaseRefreshLock();
    }

    /**
     * <h3>캐시 갱신 내부 로직</h3>
     * <p>Bulkhead + 서킷 브레이커를 통해 DB를 조회합니다.</p>
     */
    private void refreshInternal(PostCacheFlag type, List<Long> allPostIds) {

            List<PostSimpleDetail> refreshed = allPostIds.stream()
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
