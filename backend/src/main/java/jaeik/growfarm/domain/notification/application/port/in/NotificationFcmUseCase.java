package jaeik.growfarm.domain.notification.application.port.in;

/**
 * <h2>FCM 토큰 관리 유스케이스</h2>
 * <p>Firebase Cloud Messaging(FCM) 토큰 등록 및 삭제 관련 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationFcmUseCase {

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>사용자 로그인 또는 회원가입 시 FCM 토큰을 등록합니다.</p>
     *
     * @param userId   사용자 ID
     * @param fcmToken FCM 토큰 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    void registerFcmToken(Long userId, String fcmToken);

    /**
     * <h3>FCM 토큰 삭제 처리</h3>
     * <p>사용자 로그아웃 또는 탈퇴 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteFcmTokens(Long userId);

    /**
     * <h3>댓글 알림 FCM 전송</h3>
     * <p>댓글이 작성되었을 때 게시글 작성자에게 FCM 알림을 전송합니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     */
    void sendCommentNotification(Long postUserId, String commenterName);

    /**
     * <h3>롤링페이퍼 메시지 알림 FCM 전송</h3>
     * <p>롤링페이퍼에 새 메시지가 작성되었을 때 주인에게 FCM 알림을 전송합니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     */
    void sendPaperPlantNotification(Long farmOwnerId);

    /**
     * <h3>인기글 등극 알림 FCM 전송</h3>
     * <p>게시글이 인기글로 등극했을 때 작성자에게 FCM 알림을 전송합니다.</p>
     *
     * @param userId  사용자 ID
     * @param title   알림 제목
     * @param body    알림 내용
     */
    void sendPostFeaturedNotification(Long userId, String title, String body);
}
