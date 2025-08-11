package jaeik.growfarm.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <h2>인기글 등극 이벤트</h2>
 * <p>
 * 게시글이 주간 인기글이나 명예의 전당에 등극했을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
public class PostFeaturedEvent extends ApplicationEvent {

    /**
     * 게시글 작성자 ID (알림을 받을 사용자)
     */
    private final Long userId;

    /**
     * SSE 알림 메시지
     */
    private final String sseMessage;

    /**
     * 게시글 ID
     */
    private final Long postId;

    /**
     * FCM 알림 제목
     */
    private final String fcmTitle;

    /**
     * FCM 알림 내용
     */
    private final String fcmBody;

    /**
     * <h3>PostFeaturedEvent 생성자</h3>
     * <p>
     * 인기글 등극 이벤트를 생성한다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param userId 게시글 작성자 ID (알림을 받을 사용자)
     * @param sseMessage SSE 알림 메시지
     * @param postId 게시글 ID
     * @param fcmTitle FCM 알림 제목
     * @param fcmBody FCM 알림 내용
     * @author Jaeik
     * @since 2.0.0
     */
    public PostFeaturedEvent(Object source, Long userId, String sseMessage, Long postId, String fcmTitle, String fcmBody) {
        super(source);
        this.userId = userId;
        this.sseMessage = sseMessage;
        this.postId = postId;
        this.fcmTitle = fcmTitle;
        this.fcmBody = fcmBody;
    }
}