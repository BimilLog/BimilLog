package jaeik.growfarm.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

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
public class CommentCreatedEvent extends ApplicationEvent {

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

    /**
     * <h3>CommentCreatedEvent 생성자</h3>
     * <p>
     * 댓글 작성 이벤트를 생성한다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param postUserId 게시글 작성자 ID (알림을 받을 사용자)
     * @param commenterName 댓글 작성자 이름
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentCreatedEvent(Object source, Long postUserId, String commenterName, Long postId) {
        super(source);
        this.postUserId = postUserId;
        this.commenterName = commenterName;
        this.postId = postId;
    }

}