package jaeik.growfarm.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>댓글 작성 이벤트</h2>
 * <p>
 * 댓글이 작성되었을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public class CommentCreatedEvent {

    /**
     * 게시글 작성자 ID (알림을 받을 사용자)
     */
    private final Long postUserId;

    /**
     * 댓글 작성자 이름
     */
    private final String commenterName;

    /**
     * 게시글 ID
     */
    private final Long postId;

}