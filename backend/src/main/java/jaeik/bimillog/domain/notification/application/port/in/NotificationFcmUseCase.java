package jaeik.bimillog.domain.notification.application.port.in;

/**
 * <h2>FCM 푸시 알림 요구사항</h2>
 * <p>
 * 헥사고날 아키텍처에서 Firebase Cloud Messaging(FCM)을 통한 모바일 푸시 알림을 정의하는 Primary Port입니다.
 * FCM 토큰 관리와 다양한 이벤트 시점의 푸시 알림 전송에 대한 비즈니스 인터페이스를 제공합니다.
 * </p>
 * <p>
 * SSE 실시간 알림과 달리 모바일 앱에서 백그라운드 상태일 때도 사용자에게 알림을 전달할 수 있습니다.
 * 댓글 작성, 롤링페이퍼 메시지 작성, 인기글 등극 등의 이벤트 발생 시 즉시 푸시 알림을 전송합니다.
 * </p>
 * <p>NotificationFcmController와 각종 이벤트 리스너에서 호출합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationFcmUseCase {

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>클라이언트 앱에서 생성한 FCM 토큰을 서버에 등록하여 푸시 알림 발송을 준비합니다.</p>
     * <p>기존 토큰과 중복 검사를 수행하며, 동일한 토큰이 존재할 경우 업데이트 시간만 갱신합니다.</p>
     * <p>다중 기기 지원을 위해 한 사용자당 여러 FCM 토큰을 저장할 수 있습니다.</p>
     * <p>NotificationFcmController에서 클라이언트의 토큰 등록 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param userId   사용자 ID
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @author Jaeik
     * @since 2.0.0
     */
    void registerFcmToken(Long userId, String fcmToken);

    /**
     * <h3>FCM 토큰 삭제 처리</h3>
     * <p>사용자 로그아웃 또는 회원탈퇴 시 해당 사용자의 모든 FCM 토큰을 제거합니다.</p>
     * <p>개인정보 보호를 위해 사용자와 연관된 모든 토큰 정보를 완전히 삭제합니다.</p>
     * <p>NotificationRemoveListener에서 사용자 탈퇴 이벤트 발생 시 호출합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteFcmTokens(Long userId);

    /**
     * <h3>댓글 알림 FCM 전송</h3>
     * <p>댓글 작성 완료 시 게시글 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>댓글 작성자와 게시글 작성자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>게시글 작성자의 모든 FCM 토큰에 대해 Firebase를 통해 푸시 알림을 발송합니다.</p>
     * <p>CommentNotificationListener에서 댓글 작성 이벤트 발생 시 호출합니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @author Jaeik
     * @since 2.0.0
     */
    void sendCommentNotification(Long postUserId, String commenterName);

    /**
     * <h3>롤링페이퍼 메시지 알림 FCM 전송</h3>
     * <p>롤링페이퍼에 새 메시지 작성 완료 시 롤링페이퍼 소유자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>메시지 작성자와 롤링페이퍼 소유자가 동일한 경우 알림을 전송하지 않습니다.</p>
     * <p>롤링페이퍼 소유자의 모든 FCM 토큰에 대해 Firebase를 통해 푸시 알림을 발송합니다.</p>
     * <p>PaperNotificationListener에서 롤링페이퍼 메시지 작성 이벤트 발생 시 호출합니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPaperPlantNotification(Long farmOwnerId);

    /**
     * <h3>인기글 등극 알림 FCM 전송</h3>
     * <p>게시글이 인기글로 선정되었을 때 게시글 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>조회수, 댓글 수, 좋아요 수 등의 기준을 만족한 게시글에 대해 자동으로 발송됩니다.</p>
     * <p>게시글 작성자의 모든 FCM 토큰에 대해 Firebase를 통해 푸시 알림을 발송합니다.</p>
     * <p>PostFeaturedListener에서 인기글 등극 이벤트 발생 시 호출합니다.</p>
     *
     * @param userId  사용자 ID
     * @param title   알림 제목
     * @param body    알림 내용
     * @author Jaeik
     * @since 2.0.0
     */
    void sendPostFeaturedNotification(Long userId, String title, String body);
}
