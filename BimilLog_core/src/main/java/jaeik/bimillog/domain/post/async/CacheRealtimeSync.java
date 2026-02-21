package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * <h2>실시간 인기글 점수 업데이트</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점/-3점, 추천: +4점/-4점</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "실시간 인기글 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheRealtimeSync {
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RedisPostListUpdateAdapter redisPostListUpdateAdapter;
    private final RedisPostViewAdapter redisPostViewAdapter;
    private final PostRepository postRepository;

    private static final double COMMENT_SCORE = 3.0;
    private static final double VIEW_SCORE = 2.0;

    /**
     * <h3>실시간 인기글 점수 업데이트</h3>
     */
    @Async("realtimeEventExecutor")
    public void updateRealtimeScore(Long postId, double score) {
        redisPostRealTimeAdapter.incrementRealtimePopularScore(postId, score);
    }

    /**
     * <h3>게시글 상세 조회 — 조회수 + 실시간 점수 상승</h3>
     */
    @Async("realtimeEventExecutor")
    public void postDetailCheck(Long postId, String viewerKey) {
        try {
            redisPostRealTimeAdapter.incrementRealtimePopularScore(postId, VIEW_SCORE);
            redisPostViewAdapter.markViewedAndIncrement(postId, viewerKey);
        } catch (Exception e) {
            log.warn("상세글 조회 점수 상승 실패: postId={}, error={}", postId, e.getMessage());
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
     * <p>ZSet과 LIST의 ID 순서가 불일치할 때 호출됩니다.</p>
     * <p>현재 LIST에서 oldIds를 추출하여 새로 들어온 글과 빠진 글을 판별합니다.</p>
     *
     */
    @Async("cacheRefreshExecutor")
    public void asyncRebuildRealtimeCache(List<Long> newIds) {
        List<PostSimpleDetail> dbPosts = postRepository.findAllByIds(newIds).stream()
                .map(PostSimpleDetail::from).toList();

        if (dbPosts.isEmpty()) {
            return;
        }

        redisPostListUpdateAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, dbPosts, RedisKey.DEFAULT_CACHE_TTL);
    }
}
