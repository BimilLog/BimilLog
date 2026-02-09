package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

/**
 * <h2>실시간 인기글 점수 업데이트 및 비동기 캐시 갱신</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점/-3점, 추천: +4점/-4점</p>
 * <p>비동기 처리를 통해 이벤트 발행자와 독립적으로 실행됩니다.</p>
 * <p>HASH-ZSET 불일치 감지 시 비동기 락 기반 HASH 갱신을 수행합니다.</p>
 * <p>스케줄 기반 캐시 갱신은 {@link jaeik.bimillog.domain.post.scheduler.RealTimePostScheduler}가 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "실시간 인기글 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimePostSync {
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RedisSimplePostAdapter redisSimplePostAdapter;
    private final PostQueryRepository postQueryRepository;

    private static final double VIEW_SCORE = 2.0;
    private static final double COMMENT_SCORE = 3.0;
    private static final double LIKE_SCORE = 4.0;

    /**
     * <h3>조회 시 HASH-ZSET 불일치 감지 → 비동기 HASH 갱신</h3>
     * <p>조회 경로에서 HASH와 ZSET의 글 ID가 불일치할 때 호출됩니다.</p>
     * <p>분산 락을 획득한 뒤 ZSET에서 인기글 ID를 조회하고 DB에서 상세 정보를 가져와 HASH를 갱신합니다.</p>
     * <p>락 획득 실패 시 다른 스레드가 이미 갱신 중이므로 스킵합니다.</p>
     */
    @Async("cacheRefreshPool")
    public void asyncRefreshRealtimeWithLock(List<Long> zsetPostIds) {
        String lockValue = redisSimplePostAdapter.tryAcquireRealtimeRefreshLock();
        if (lockValue == null) {
            return;
        }

        try {
            List<PostSimpleDetail> posts = zsetPostIds.stream()
                    .map(postId -> postQueryRepository.findPostDetail(postId, null).orElse(null))
                    .filter(Objects::nonNull)
                    .map(PostDetail::toSimpleDetail)
                    .toList();
            if (posts.isEmpty()) {
                return;
            }
            redisSimplePostAdapter.cachePostsWithTtl(RedisKey.REALTIME_SIMPLE_KEY, posts, null);
        } catch (Exception e) {
            log.warn("실시간 인기글 해시 갱신 실패: {}", e.getMessage());
        } finally {
            redisSimplePostAdapter.releaseRealtimeRefreshLock(lockValue);
        }
    }

    /**
     * <h3>게시글 조회 이벤트 처리</h3>
     * <p>게시글 조회 시 해당 게시글의 실시간 인기글 점수를 2점 증가시킵니다.</p>
     *
     * @param postId 조회된 게시글 ID
     */
    @Async("realtimeEventExecutor")
    public void handlePostViewed(Long postId) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(postId, VIEW_SCORE);
    }

    /**
     * <h3>게시글 추천 이벤트 처리</h3>
     * <p>게시글 추천 시 실시간 인기글 점수를 4점 증가시킵니다.</p>
     *
     * @param postId 추천된 게시글 ID
     */
    @Async("realtimeEventExecutor")
    public void handlePostLiked(Long postId) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(postId, LIKE_SCORE);
    }

    /**
     * <h3>게시글 추천 취소 이벤트 처리</h3>
     * <p>게시글 추천 취소 시 실시간 인기글 점수를 4점 감소시킵니다.</p>
     *
     * @param postId 추천 취소된 게시글 ID
     */
    @Async("realtimeEventExecutor")
    public void handlePostUnliked(Long postId) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(postId, -LIKE_SCORE);
    }

    /**
     * <h3>댓글 작성 이벤트 처리</h3>
     * <p>댓글 작성 시 해당 게시글의 실시간 인기글 점수를 3점 증가시킵니다.</p>
     *
     * @param event 댓글 작성 이벤트
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.getPostId(), COMMENT_SCORE);
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     * <p>댓글 삭제 시 해당 게시글의 실시간 인기글 점수를 3점 감소시킵니다.</p>
     *
     * @param event 댓글 삭제 이벤트
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentDeleted(CommentDeletedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.postId(), -COMMENT_SCORE);
    }
}
