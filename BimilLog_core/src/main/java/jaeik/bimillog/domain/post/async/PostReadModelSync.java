package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.jpa.PostReadModel;
import jaeik.bimillog.domain.post.entity.jpa.PostReadModelEventType;
import jaeik.bimillog.domain.post.entity.jpa.ProcessedEvent;
import jaeik.bimillog.domain.post.repository.PostReadModelRepository;
import jaeik.bimillog.domain.post.repository.ProcessedEventRepository;
import jaeik.bimillog.domain.post.service.PostReadModelDlqService;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * <h2>Post Read Model 동기화 서비스</h2>
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
public class PostReadModelSync {
    private final PostReadModelRepository postReadModelRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PostReadModelDlqService postReadModelDlqService;

    /**
     * 멱등성 체크 - 이미 처리된 이벤트인지 확인
     * @return true면 이미 처리됨 (스킵해야 함)
     */
    private boolean isAlreadyProcessed(String eventId) {
        if (processedEventRepository.existsById(eventId)) {
            log.debug("이미 처리된 이벤트 스킵: {}", eventId);
            return true;
        }
        return false;
    }

    /**
     * <h3>게시글 생성 이벤트 처리</h3>
     * <p>PostReadModel에 새 레코드를 INSERT합니다.</p>
     */
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            recover = "recoverPostCreated"
    )
    @Transactional
    public void handlePostCreated(Long postId, String title, Long memberId, String memberName, Instant createdAt) {
        String eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        if (isAlreadyProcessed(eventId)) return;

        PostReadModel readModel = PostReadModel.createNew(postId, title, memberId, memberName, createdAt);

        postReadModelRepository.save(readModel);
        processedEventRepository.save(new ProcessedEvent(eventId, "POST_CREATED"));

        log.debug("PostReadModel 생성 완료: postId={}", postId);
    }

    @Recover
    public void recoverPostCreated(Exception e, Long postId, String title, Long memberId, String memberName, String eventId) {
        log.error("PostReadModel 생성 최종 실패: postId={}", postId, e);
        postReadModelDlqService.savePostCreated(
                eventId,
                postId,
                title,
                memberId,
                memberName
        );
    }

    /**
     * <h3>게시글 수정 이벤트 처리</h3>
     * <p>PostReadModel의 제목을 업데이트합니다.</p>
     */
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            recover = "recoverPostUpdated"
    )
    @Transactional
    public void handlePostUpdated(Long postId, String newTitle) {
        String eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        if (isAlreadyProcessed(eventId)) return;

        postReadModelRepository.findById(postId).ifPresent(readModel -> readModel.updateTitle(newTitle));
        processedEventRepository.save(new ProcessedEvent(eventId, "POST_UPDATED"));

        log.debug("PostReadModel 제목 업데이트 완료: postId={}", postId);
    }

    @Recover
    public void recoverPostUpdated(Exception e, Long postId, String newTitle) {
        log.error("PostReadModel 제목 업데이트 최종 실패: postId={}", postId, e);
        postReadModelDlqService.savePostUpdated(
                UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                postId,
                newTitle
        );
    }

    /**
     * <h3>게시글 좋아요 이벤트 처리</h3>
     * <p>PostReadModel의 like_count를 +1 합니다.</p>
     */
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            recover = "recoverPostLiked"
    )
    @Transactional
    public void handlePostLiked(Long postId) {
        String eventId = "LIKE_INC:" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        if (isAlreadyProcessed(eventId)) return;

        postReadModelRepository.incrementLikeCount(postId);
        processedEventRepository.save(new ProcessedEvent(eventId, "LIKE_INCREMENT"));

        log.debug("PostReadModel 좋아요 수 증가 완료: postId={}", postId);
    }

    @Recover
    public void recoverPostLiked(Exception e, Long postId) {
        log.error("PostReadModel 좋아요 수 증가 최종 실패: postId={}", postId, e);
        postReadModelDlqService.saveSimpleEvent(
                PostReadModelEventType.LIKE_INCREMENT,
                "LIKE_INC:" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                postId
        );
    }

    /**
     * <h3>게시글 좋아요 취소 이벤트 처리</h3>
     * <p>PostReadModel의 like_count를 -1 합니다.</p>
     * <p>멱등성 체크 없이 실행 (중복 실행해도 DB에서 안전하게 처리)</p>
     */
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            recover = "recoverPostUnliked"
    )
    @Transactional
    public void handlePostUnliked(Long postId) {
        postReadModelRepository.decrementLikeCount(postId);
        log.debug("PostReadModel 좋아요 수 감소 완료: postId={}", postId);
    }

    @Recover
    public void recoverPostUnliked(Exception e, Long postId) {
        log.error("PostReadModel 좋아요 수 감소 최종 실패: postId={}", postId, e);
        String eventId = "LIKE_DEC:" + postId + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        postReadModelDlqService.saveSimpleEvent(PostReadModelEventType.LIKE_DECREMENT, eventId, postId);
    }

    /**
     * <h3>댓글 생성 이벤트 처리</h3>
     * <p>PostReadModel의 comment_count를 +1 합니다.</p>
     */
    @TransactionalEventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void handleCommentCreated(CommentCreatedEvent event) {
        String eventId = "CMT_INC:" + event.getEventId();

        if (isAlreadyProcessed(eventId)) return;

        postReadModelRepository.incrementCommentCount(event.getPostId());
        processedEventRepository.save(new ProcessedEvent(eventId, "COMMENT_INCREMENT"));

        log.debug("PostReadModel 댓글 수 증가 완료: postId={}", event.getPostId());
    }

    @Recover
    public void recoverCommentCreated(Exception e, CommentCreatedEvent event) {
        log.error("PostReadModel 댓글 수 증가 최종 실패: postId={}", event.getPostId(), e);
        postReadModelDlqService.saveSimpleEvent(
                PostReadModelEventType.COMMENT_INCREMENT,
                "CMT_INC:" + event.getEventId(),
                event.getPostId()
        );
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     * <p>PostReadModel의 comment_count를 -1 합니다.</p>
     * <p>멱등성 체크 없이 실행 (중복 실행해도 DB에서 안전하게 처리)</p>
     */
    @TransactionalEventListener
    @Async("postCQRSEventExecutor")
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void handleCommentDeleted(CommentDeletedEvent event) {
        postReadModelRepository.decrementCommentCount(event.postId());
        log.debug("PostReadModel 댓글 수 감소 완료: postId={}", event.postId());
    }

    @Recover
    public void recoverCommentDeleted(Exception e, CommentDeletedEvent event) {
        log.error("PostReadModel 댓글 수 감소 최종 실패: postId={}", event.postId(), e);
        String eventId = "CMT_DEC:" + event.postId() + ":" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        postReadModelDlqService.saveSimpleEvent(PostReadModelEventType.COMMENT_DECREMENT, eventId, event.postId());
    }
}
