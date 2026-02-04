package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.jpa.PostReadModel;
import jaeik.bimillog.domain.post.entity.jpa.ProcessedEvent;
import jaeik.bimillog.domain.post.event.PostCreatedEvent;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.domain.post.event.PostUpdatedEvent;
import jaeik.bimillog.domain.post.repository.PostReadModelRepository;
import jaeik.bimillog.domain.post.repository.ProcessedEventRepository;
import jaeik.bimillog.domain.post.service.PostReadModelDlqService;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * <h2>Post Read Model 동기화 리스너</h2>
 * <p>게시글 관련 이벤트를 수신하여 PostReadModel을 동기화합니다.</p>
 * <p>비동기 + 재시도 + Recover 패턴을 사용합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Log(logResult = false, message = "Post Read Model 동기화")
@Component
@RequiredArgsConstructor
@Slf4j
public class PostReadModelSyncListener {

    private final PostReadModelRepository postReadModelRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PostReadModelDlqService postReadModelDlqService;

    /**
     * <h3>게시글 생성 이벤트 처리</h3>
     * <p>PostReadModel에 새 레코드를 INSERT합니다.</p>
     */
    @EventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    @Transactional
    public void handlePostCreated(PostCreatedEvent event) {
        String eventId = event.getIdempotencyKey();

        // 멱등성 체크
        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return;
        }

        PostReadModel readModel = PostReadModel.builder()
                .postId(event.postId())
                .title(event.title())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .memberId(event.memberId())
                .memberName(event.memberName() != null ? event.memberName() : "익명")
                .createdAt(event.createdAt())
                .build();

        postReadModelRepository.save(readModel);
        processedEventRepository.save(new ProcessedEvent(eventId, "POST_CREATED"));

        log.debug("PostReadModel 생성 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverPostCreated(Exception e, PostCreatedEvent event) {
        log.error("PostReadModel 생성 최종 실패: postId={}", event.postId(), e);
        postReadModelDlqService.savePostCreated(
                event.getIdempotencyKey(),
                event.postId(),
                event.title(),
                event.memberId(),
                event.memberName()
        );
    }

    /**
     * <h3>게시글 수정 이벤트 처리</h3>
     * <p>PostReadModel의 제목을 업데이트합니다.</p>
     */
    @EventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    @Transactional
    public void handlePostUpdated(PostUpdatedEvent event) {
        String eventId = event.getIdempotencyKey();

        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return;
        }

        postReadModelRepository.updateTitle(event.postId(), event.newTitle());
        processedEventRepository.save(new ProcessedEvent(eventId, "POST_UPDATED"));

        log.debug("PostReadModel 제목 업데이트 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverPostUpdated(Exception e, PostUpdatedEvent event) {
        log.error("PostReadModel 제목 업데이트 최종 실패: postId={}", event.postId(), e);
        postReadModelDlqService.savePostUpdated(
                event.getIdempotencyKey(),
                event.postId(),
                event.newTitle()
        );
    }

    /**
     * <h3>게시글 좋아요 이벤트 처리</h3>
     * <p>PostReadModel의 like_count를 +1 합니다.</p>
     */
    @EventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    @Transactional
    public void handlePostLiked(PostLikeEvent event) {
        String eventId = "LIKE_INC:" + event.getIdempotencyKey();

        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return;
        }

        postReadModelRepository.incrementLikeCount(event.postId());
        processedEventRepository.save(new ProcessedEvent(eventId, "LIKE_INCREMENT"));

        log.debug("PostReadModel 좋아요 수 증가 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverPostLiked(Exception e, PostLikeEvent event) {
        log.error("PostReadModel 좋아요 수 증가 최종 실패: postId={}", event.postId(), e);
        postReadModelDlqService.saveLikeIncrement(
                "LIKE_INC:" + event.getIdempotencyKey(),
                event.postId()
        );
    }

    /**
     * <h3>게시글 좋아요 취소 이벤트 처리</h3>
     * <p>PostReadModel의 like_count를 -1 합니다.</p>
     */
    @EventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    @Transactional
    public void handlePostUnliked(PostUnlikeEvent event) {
        // PostUnlikeEvent는 eventId가 없으므로 새로 생성
        String eventId = "LIKE_DEC:" + event.postId() + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return;
        }

        postReadModelRepository.decrementLikeCount(event.postId());
        processedEventRepository.save(new ProcessedEvent(eventId, "LIKE_DECREMENT"));

        log.debug("PostReadModel 좋아요 수 감소 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverPostUnliked(Exception e, PostUnlikeEvent event) {
        log.error("PostReadModel 좋아요 수 감소 최종 실패: postId={}", event.postId(), e);
        String eventId = "LIKE_DEC:" + event.postId() + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        postReadModelDlqService.saveLikeDecrement(eventId, event.postId());
    }

    /**
     * <h3>댓글 생성 이벤트 처리</h3>
     * <p>PostReadModel의 comment_count를 +1 합니다.</p>
     */
    @EventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    @Transactional
    public void handleCommentCreated(CommentCreatedEvent event) {
        String eventId = "CMT_INC:" + event.getIdempotencyKey();

        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return;
        }

        postReadModelRepository.incrementCommentCount(event.postId());
        processedEventRepository.save(new ProcessedEvent(eventId, "COMMENT_INCREMENT"));

        log.debug("PostReadModel 댓글 수 증가 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverCommentCreated(Exception e, CommentCreatedEvent event) {
        log.error("PostReadModel 댓글 수 증가 최종 실패: postId={}", event.postId(), e);
        postReadModelDlqService.saveCommentIncrement(
                "CMT_INC:" + event.getIdempotencyKey(),
                event.postId()
        );
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     * <p>PostReadModel의 comment_count를 -1 합니다.</p>
     */
    @EventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    @Transactional
    public void handleCommentDeleted(CommentDeletedEvent event) {
        // CommentDeletedEvent는 eventId가 없으므로 새로 생성
        String eventId = "CMT_DEC:" + event.postId() + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return;
        }

        postReadModelRepository.decrementCommentCount(event.postId());
        processedEventRepository.save(new ProcessedEvent(eventId, "COMMENT_DECREMENT"));

        log.debug("PostReadModel 댓글 수 감소 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverCommentDeleted(Exception e, CommentDeletedEvent event) {
        log.error("PostReadModel 댓글 수 감소 최종 실패: postId={}", event.postId(), e);
        String eventId = "CMT_DEC:" + event.postId() + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        postReadModelDlqService.saveCommentDecrement(eventId, event.postId());
    }
}
