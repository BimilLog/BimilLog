package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.global.event.FriendInteractionEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * <h2>댓글 추천 이벤트</h2>
 * <p>댓글이 추천(좋아요)되었을 때 발생하는 비동기 이벤트</p>
 * <p>CommentCommandService에서 댓글 추천 시 발생합니다.</p>
 * <p>상호작용 점수 증가에 사용됩니다.</p>
 * <p>추천 취소 시에는 이벤트를 발행하지 않습니다 (점수 유지).</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeEvent implements FriendInteractionEvent {
    private String eventId;
    private Long commentId;
    private Long commentAuthorId;
    private Long likerId;

    public CommentLikeEvent(Long commentId, Long commentAuthorId, Long likerId) {
        this.commentId = commentId;
        this.commentAuthorId = commentAuthorId;
        this.likerId = likerId;
        this.eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    public Long getMemberId() {
        return likerId;
    }

    @Override
    public Long getTargetMemberId() {
        return commentAuthorId;
    }

    @Override
    public String getIdempotencyKey() {
        return eventId;
    }

    @Override
    public void getAlreadyProcess() {
        log.info("이미 처리된 댓글 좋아요 이벤트: commentId={}, idempotencyKey={}", commentId, eventId);

    }

    @Override
    public void getDlqMessage(Exception e) {
        log.warn("댓글 좋아요 상호작용 점수 증가 실패 DLQ 진입: commentId={}, authorId={}, likerId={}", commentId, commentAuthorId, likerId, e);

    }
}
