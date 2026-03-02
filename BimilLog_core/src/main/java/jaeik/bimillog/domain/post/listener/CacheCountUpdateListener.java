package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.event.PostLikedEvent;
import jaeik.bimillog.domain.post.event.PostUnlikedEvent;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <h2>게시글 카운터 캐시 증감</h2>
 * <p>추천/댓글 이벤트를 받아 모든 JSON LIST의 카운터를 비동기로 증분합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "카운터 증감")
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheCountUpdateListener {
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;

    /**
     * <h3>게시글 추천 이벤트 처리 — 좋아요 카운터 +1</h3>
     */
    @TransactionalEventListener
    @Async("cacheCountUpdateExecutor")
    public void handlePostLiked(PostLikedEvent event) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(event.getPostId(), "likeCount", 1);
    }

    /**
     * <h3>게시글 추천취소 이벤트 처리 — 좋아요 카운터 -1</h3>
     */
    @TransactionalEventListener
    @Async("cacheCountUpdateExecutor")
    public void handlePostUnliked(PostUnlikedEvent event) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(event.postId(), "likeCount", -1);
    }

    /**
     * <h3>댓글 작성 이벤트 처리 — 댓글 카운터 +1</h3>
     */
    @TransactionalEventListener
    @Async("cacheCountUpdateExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(event.getPostId(), "commentCount", 1);
    }

    /**
     * <h3>댓글 삭제 이벤트 처리 — 댓글 카운터 -1</h3>
     */
    @TransactionalEventListener
    @Async("cacheCountUpdateExecutor")
    public void handleCommentDeleted(CommentDeletedEvent event) {
        redisPostListUpdateAdapter.incrementCounterInAllLists(event.postId(), "commentCount", -1);
    }
}
