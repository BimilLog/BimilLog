package jaeik.growfarm.domain.notification.application.port.in;

/**
 * <h2>알림 이벤트 유스케이스</h2>
 * <p>다양한 이벤트 기반 알림 전송 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationEventUseCase {

    /**
     * <h3>댓글 알림 전송</h3>
     * <p>댓글이 작성되었을 때 게시글 작성자에게 알림을 전송합니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     */
    void sendCommentNotification(Long postUserId, String commenterName, Long postId);

    /**
     * <h3>롤링페이퍼 메시지 알림 전송</h3>
     * <p>롤링페이퍼에 새 메시지가 작성되었을 때 주인에게 알림을 전송합니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     */
    void sendPaperPlantNotification(Long farmOwnerId, String userName);

    /**
     * <h3>인기글 등극 알림 전송</h3>
     * <p>게시글이 인기글로 등극했을 때 작성자에게 알림을 전송합니다.</p>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     */
    void sendPostFeaturedNotification(Long userId, String message, Long postId);


}