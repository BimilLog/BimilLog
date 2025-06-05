package jaeik.growfarm.global.event;

import jaeik.growfarm.entity.user.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>인기댓글 등극 이벤트</h2>
 * <p>
 * 댓글이 인기댓글로 선정되었을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class CommentFeaturedEvent {

    /**
     * 댓글 작성자 ID (알림을 받을 사용자)
     */
    private final Long userId;

    /**
     * 게시글 ID
     */
    private final Long postId;

    /**
     * 댓글 작성자 엔티티 (FCM 토큰 조회용)
     */
    private final Users user;
}