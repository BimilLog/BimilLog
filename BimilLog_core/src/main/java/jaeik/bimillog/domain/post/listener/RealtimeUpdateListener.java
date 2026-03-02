package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostDetailViewedEvent;
import jaeik.bimillog.domain.post.event.PostLikedEvent;
import jaeik.bimillog.domain.post.event.PostUnlikedEvent;
import jaeik.bimillog.domain.post.event.RealtimeCacheRebuildEvent;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeUpdateListener {
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RedisPostViewAdapter redisPostViewAdapter;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final PostRepository postRepository;

    private static final double LIKE_SCORE = 4.0;
    private static final double COMMENT_SCORE = 3.0;
    private static final double VIEW_SCORE = 2.0;

    /**
     * <h3>게시글 추천 이벤트 처리 — 실시간 점수 +4.0</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handlePostLiked(PostLikedEvent event) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(event.getPostId(), LIKE_SCORE);
    }

    /**
     * <h3>게시글 추천취소 이벤트 처리 — 실시간 점수 -4.0</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handlePostUnliked(PostUnlikedEvent event) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(event.postId(), -LIKE_SCORE);
    }

    /**
     * <h3>게시글 상세 조회 이벤트 처리 — 조회수 + 실시간 점수 상승</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handlePostDetailViewed(PostDetailViewedEvent event) {
        try {
            redisPostRealTimeAdapter.incrementRealtimePopularScore(event.postId(), VIEW_SCORE);
            redisPostViewAdapter.markViewedAndIncrement(event.postId(), event.viewerKey());
        } catch (Exception e) {
            log.warn("상세글 조회 점수 상승 실패: postId={}, error={}", event.postId(), e.getMessage());
        }
    }

    /**
     * <h3>댓글 작성 이벤트 처리</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(event.getPostId(), COMMENT_SCORE);
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentDeleted(CommentDeletedEvent event) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(event.postId(), -COMMENT_SCORE);
    }

    /**
     * <h3>실시간 인기글 JSON LIST 비동기 갱신</h3>
     * <p>ZSet과 LIST의 ID 순서가 불일치할 때 이벤트를 수신하여 갱신합니다.</p>
     * <p>호출측(RealtimePostCacheService)에 트랜잭션이 없으므로 @EventListener 사용</p>
     */
    @EventListener
    @Async("cacheRefreshExecutor")
    public void handleRealtimeCacheRebuild(RealtimeCacheRebuildEvent event) {
        List<PostSimpleDetail> dbPosts = postRepository.findAllByIds(event.postIds()).stream().map(PostSimpleDetail::from).toList();
        if (dbPosts.isEmpty()) return;
        redisPostListUpdateAdapter.replaceList(RedisKey.POST_REALTIME_JSON_KEY, dbPosts, RedisKey.DEFAULT_CACHE_TTL);
    }
}
