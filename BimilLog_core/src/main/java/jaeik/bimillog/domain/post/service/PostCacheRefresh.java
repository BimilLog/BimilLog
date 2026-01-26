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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <h2>글 캐시 갱신 클래스</h2>
 * <p>인기글(공지/실시간/주간/레전드) 목록 캐시의 비동기 갱신을 담당합니다.</p>
 * <p>PER(Probabilistic Early Refresh) 기반으로 목록 API에서 호출됩니다.</p>
 * <p>DB 조회 후 {@link PostCacheRefreshExecutor}에 캐시 저장을 위임합니다.</p>
 *
 * @author Jaeik
 * @version 2.9.0
 */
@Log(logResult = false, message = "캐시 갱신")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefresh {
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final PostCacheRefreshExecutor postCacheRefreshExecutor;
    private final PostQueryRepository postQueryRepository;
    private final FeaturedPostRepository featuredPostRepository;
    private final DbFallbackGateway dbFallbackGateway;

    /**
     * <h3>실시간 인기글 캐시 비동기 갱신 (락 기반)</h3>
     * <p>비동기 스레드에서 SET NX 락을 획득하고, 동기 스레드에서 이미 조회한 데이터로 캐시를 저장합니다.</p>
     * <p>DB 재조회 없이 전달받은 PostSimpleDetail 목록을 그대로 캐시에 저장합니다.</p>
     * <p>락 획득 실패 시 다른 스레드가 갱신 중이므로 스킵합니다.</p>
     *
     * @param posts 동기 스레드에서 이미 DB 조회한 실시간 인기글 목록
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshRealtimeWithLock(List<PostSimpleDetail> posts) {
        if (!redisSimplePostAdapter.tryAcquireRefreshLock(PostCacheFlag.REALTIME)) {
            return;
        }
        try {
            postCacheRefreshExecutor.cachePosts(PostCacheFlag.REALTIME, posts);
        } finally {
            redisSimplePostAdapter.releaseRefreshLock(PostCacheFlag.REALTIME);
        }
    }

    /**
     * <h3>주간/레전드/공지 캐시 비동기 갱신 (락 기반)</h3>
     * <p>비동기 스레드에서 SET NX 락을 획득하고 featured_post -> DB 조회 -> 캐시 갱신합니다.</p>
     * <p>락 획득 실패 시 다른 스레드가 갱신 중이므로 스킵합니다.</p>
     *
     * @param type 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     */
    @Async("cacheRefreshExecutor")
    public void asyncRefreshFeaturedWithLock(PostCacheFlag type) {
        if (!redisSimplePostAdapter.tryAcquireRefreshLock(type)) {
            return;
        }
        try {
            List<PostSimpleDetail> posts = queryPostsByType(type, List.of());
            postCacheRefreshExecutor.cachePosts(type, posts);
        } finally {
            redisSimplePostAdapter.releaseRefreshLock(type);
        }
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
        List<PostSimpleDetail> posts = queryPostsByType(type, allPostIds);
        postCacheRefreshExecutor.cachePosts(type, posts);
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
                .map(postId -> dbFallbackGateway.executeDetail(
                        FallbackType.POPULAR_DETAIL,
                        postId,
                        () -> postQueryRepository.findPostDetail(postId, null)
                ).orElse(null))
                .filter(Objects::nonNull)
                .map(PostDetail::toSimpleDetail)
                .toList();
    }
}
