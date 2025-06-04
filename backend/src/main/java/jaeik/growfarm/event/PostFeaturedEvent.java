package jaeik.growfarm.event;

import jaeik.growfarm.entity.user.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>인기글 등극 이벤트</h2>
 * <p>
 * 게시글이 주간 인기글이나 명예의 전당에 등극했을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class PostFeaturedEvent {

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
     * 게시글 작성자 엔티티 (FCM 토큰 조회용)
     */
    private final Users user;

    /**
     * FCM 알림 제목
     */
    private final String fcmTitle;

    /**
     * FCM 알림 내용
     */
    private final String fcmBody;
}