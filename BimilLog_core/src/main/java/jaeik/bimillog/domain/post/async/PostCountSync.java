package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <h2>게시글 조회수 증가 리스너</h2>
 * <p>게시글 조회 이벤트를 수신하여 중복 조회 방지 후 조회수를 증가시킵니다.</p>
 * <p>Redis SET으로 24시간 중복 조회를 방지하고, Redis Hash에 조회수를 버퍼링합니다.</p>
 * <p>글 단위 Hash 캐시는 1분 플러시 스케줄러에서 일괄 반영합니다.</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "조회수 증가")
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCountSync {
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;
    private final PostRepository postRepository;

    /**
     * <h3>게시글 조회 이벤트 처리 (원자적)</h3>
     * <p>Lua 스크립트로 중복 확인 + 마킹 + 조회수 증가를 원자적으로 처리합니다.</p>
     * <p>Hash 캐시 반영은 1분 플러시 스케줄러에서 일괄 처리합니다.</p>
     * <p>동시 요청 시 Check-Then-Act 레이스 컨디션을 방지합니다.</p>
     *
     * @param postId    조회된 게시글 ID
     * @param viewerKey 조회자 식별 키 (중복 조회 방지용)
     */
    @Async("cacheCountUpdateExecutor")
    public void handlePostViewed(Long postId, String viewerKey) {
        try {
            redisPostUpdateAdapter.markViewedAndIncrement(postId, viewerKey);
        } catch (Exception e) {
            log.warn("조회수 처리 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * <h3>좋아요 수 Redis 버퍼 증감</h3>
     * <p>Hash 캐시 반영은 1분 플러시 스케줄러에서 일괄 처리합니다.</p>
     * <p>Redis 실패 시 DB에 직접 반영합니다.</p>
     */
    @Async("cacheCountUpdateExecutor")
    public void incrementLikeWithFallback(Long postId, long delta) {
        try {
            redisPostUpdateAdapter.incrementLikeBuffer(postId, delta);
        } catch (Exception e) {
            log.warn("[LIKE_FALLBACK] Redis 실패, DB 직접 반영: postId={}, error={}", postId, e.getMessage());
            if (delta > 0) {
                postRepository.incrementLikeCount(postId);
            } else {
                postRepository.decrementLikeCount(postId);
            }
        }
    }

    /**
     * <h3>댓글 작성 시 댓글 수 증가</h3>
     * <p>Hash 캐시 반영은 1분 플러시 스케줄러에서 일괄 처리합니다.</p>
     * <p>Redis 실패 시 DB에 직접 반영합니다.</p>
     */
    @Async("cacheCountUpdateExecutor")
    @TransactionalEventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        Long postId = event.getPostId();
        try {
            redisPostUpdateAdapter.incrementCommentBuffer(postId, 1);
        } catch (Exception e) {
            log.warn("[COMMENT_FALLBACK] Redis 실패, DB 직접 반영: postId={}, error={}", postId, e.getMessage());
            postRepository.incrementCommentCount(postId);
        }
    }

    /**
     * <h3>댓글 삭제 시 댓글 수 감소</h3>
     * <p>Hash 캐시 반영은 1분 플러시 스케줄러에서 일괄 처리합니다.</p>
     * <p>Redis 실패 시 DB에 직접 반영합니다.</p>
     */
    @Async("cacheCountUpdateExecutor")
    @TransactionalEventListener
    public void handleCommentDeleted(CommentDeletedEvent event) {
        Long postId = event.postId();
        try {
            redisPostUpdateAdapter.incrementCommentBuffer(postId, -1);
        } catch (Exception e) {
            log.warn("[COMMENT_FALLBACK] Redis 실패, DB 직접 반영: postId={}, error={}", postId, e.getMessage());
            postRepository.decrementCommentCount(postId);
        }
    }
}
