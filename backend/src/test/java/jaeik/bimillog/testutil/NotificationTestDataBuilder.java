package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;

import java.time.LocalDateTime;
/**
 * <h2>Notification 도메인 테스트 데이터 빌더</h2>
 * <p>알림 관련 테스트 데이터를 쉽게 생성하기 위한 빌더 클래스</p>
 * <p>다양한 알림 타입과 시나리오를 위한 헬퍼 메서드 제공</p>
 *
 * <h3>사용 예시:</h3>
 * <pre>
 * Notification notification = NotificationTestDataBuilder.aNotification()
 *     .withReceiver(testUser)
 *     .withType(NotificationType.COMMENT)
 *     .withMessage("새 댓글이 달렸습니다")
 *     .withRelatedId(postId)
 *     .asRead()
 *     .build();
 * </pre>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class NotificationTestDataBuilder {

    private Long id;
    private User receiver;
    private NotificationType type = NotificationType.COMMENT;
    private Long relatedId;
    private String message = "Test notification message";
    private boolean isRead = false;
    private final LocalDateTime createdAt = LocalDateTime.now();

    private NotificationTestDataBuilder() {}

    /**
     * 빌더 시작점
     * @return NotificationTestDataBuilder 인스턴스
     */
    public static NotificationTestDataBuilder aNotification() {
        return new NotificationTestDataBuilder();
    }

    /**
     * 댓글 알림 빌더
     * @param receiver 수신자
     * @param postId 관련 게시글 ID
     * @return NotificationTestDataBuilder 인스턴스
     */
    public static NotificationTestDataBuilder aCommentNotification(User receiver, Long postId) {
        return new NotificationTestDataBuilder()
                .withReceiver(receiver)
                .withType(NotificationType.COMMENT)
                .withRelatedId(postId)
                .withMessage("게시글에 새 댓글이 달렸습니다.");
    }

    /**
     * 좋아요 알림 빌더
     * @param receiver 수신자
     * @param postId 관련 게시글 ID
     * @return NotificationTestDataBuilder 인스턴스
     */
    public static NotificationTestDataBuilder aLikeNotification(User receiver, Long postId) {
        return new NotificationTestDataBuilder()
                .withReceiver(receiver)
                .withType(NotificationType.POST_FEATURED)
                .withRelatedId(postId)
                .withMessage("게시글이 추천되었습니다.");
    }

    /**
     * 롤링페이퍼 메시지 알림 빌더
     * @param receiver 수신자
     * @return NotificationTestDataBuilder 인스턴스
     */
    public static NotificationTestDataBuilder aPaperMessageNotification(User receiver) {
        return new NotificationTestDataBuilder()
                .withReceiver(receiver)
                .withType(NotificationType.PAPER)
                .withMessage("새로운 롤링페이퍼 메시지가 도착했습니다.");
    }

    /**
     * 관리자 공지 알림 빌더
     * @param receiver 수신자
     * @param message 공지 메시지
     * @return NotificationTestDataBuilder 인스턴스
     */
    public static NotificationTestDataBuilder anAdminNotification(User receiver, String message) {
        return new NotificationTestDataBuilder()
                .withReceiver(receiver)
                .withType(NotificationType.ADMIN)
                .withMessage(message);
    }

    public NotificationTestDataBuilder withReceiver(User receiver) {
        this.receiver = receiver;
        return this;
    }

    public NotificationTestDataBuilder withType(NotificationType type) {
        this.type = type;
        return this;
    }

    public NotificationTestDataBuilder withRelatedId(Long relatedId) {
        this.relatedId = relatedId;
        return this;
    }

    public NotificationTestDataBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 알림을 읽음 상태로 설정
     * @return NotificationTestDataBuilder 인스턴스
     */
    public NotificationTestDataBuilder asRead() {
        this.isRead = true;
        return this;
    }

    /**
     * 알림을 읽지 않음 상태로 설정
     * @return NotificationTestDataBuilder 인스턴스
     */
    public NotificationTestDataBuilder asUnread() {
        this.isRead = false;
        return this;
    }

    /**
     * Notification 엔티티 생성
     * @return Notification 엔티티
     */
    public Notification build() {
        if (receiver == null) {
            receiver = TestUsers.USER1; // 기본 수신자
        }

        // Create notification using the static factory method
        String url = relatedId != null ? "/post/" + relatedId : null;
        Notification notification = Notification.create(
                receiver,
                type,
                message,
                url
        );

        // Set additional fields via reflection for testing
        if (id != null) {
            TestFixtures.setFieldValue(notification, "id", id);
        }
        if (isRead) {
            TestFixtures.setFieldValue(notification, "isRead", isRead);
        }
        if (createdAt != null) {
            // LocalDateTime을 Instant로 변환
            java.time.Instant instantCreatedAt = createdAt.toInstant(java.time.ZoneOffset.UTC);
            TestFixtures.setFieldValue(notification, "createdAt", instantCreatedAt);
        }

        return notification;
    }
}