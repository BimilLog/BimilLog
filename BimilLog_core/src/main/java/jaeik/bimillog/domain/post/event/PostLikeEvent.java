package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.global.event.FriendInteractionEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * <h2>게시글 추천 이벤트</h2>
 * <p>게시글이 추천(좋아요)되었을 때 발생하는 비동기 이벤트</p>
 * <p>PostInteractionService에서 게시글 추천 토글 시 발생합니다.</p>
 * <p>실시간 인기글 점수 증가 및 상호작용 점수 증가에 사용됩니다.</p>
 * <p>추천 취소 시에는 이벤트를 발행하지 않습니다.</p>

 * @author Jaeik
 * @version 2.7.0
 */
@Slf4j
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeEvent implements FriendInteractionEvent {
    private String eventId;
    private Long postId;
    private Long postAuthorId;
    private Long likerId;

    public PostLikeEvent(Long postId, Long postAuthorId, Long likerId) {
        this.postId = postId;
        this.postAuthorId = postAuthorId;
        this.likerId = likerId;
        this.eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    public Long getMemberId() {
        return likerId;
    }

    @Override
    public Long getTargetMemberId() {
        return postAuthorId;
    }

    @Override
    public String getIdempotencyKey() {
        return eventId;
    }

    @Override
    public void getAlreadyProcess() {
        log.info("이미 처리된 게시글 좋아요 이벤트 : postId={}, idempotencyKey={}", postId, eventId);

    }

    @Override
    public void getDlqMessage(Exception e) {
        log.warn("게시글 추천 상호작용 점수 증가 실패 DLQ 진입: postId={}, authorId={}, likerId={}", postId, postAuthorId, likerId, e);

    }
}
