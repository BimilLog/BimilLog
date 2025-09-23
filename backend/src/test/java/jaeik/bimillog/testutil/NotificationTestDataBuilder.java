package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private LocalDateTime createdAt = LocalDateTime.now();

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
     * 대댓글 알림 빌더
     * @param receiver 수신자
     * @param commentId 관련 댓글 ID
     * @return NotificationTestDataBuilder 인스턴스
     */
    public static NotificationTestDataBuilder aReplyNotification(User receiver, Long commentId) {
        return new NotificationTestDataBuilder()
                .withReceiver(receiver)
                .withType(NotificationType.COMMENT) // REPLY 타입이 없으므로 COMMENT 사용
                .withRelatedId(commentId)
                .withMessage("댓글에 답글이 달렸습니다.");
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

    public NotificationTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
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

    public NotificationTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * 과거 시간으로 생성 시간 설정
     * @param daysAgo 며칠 전
     * @return NotificationTestDataBuilder 인스턴스
     */
    public NotificationTestDataBuilder createdDaysAgo(int daysAgo) {
        this.createdAt = LocalDateTime.now().minusDays(daysAgo);
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
            setFieldValue(notification, "id", id);
        }
        if (isRead) {
            setFieldValue(notification, "isRead", isRead);
        }
        if (createdAt != null) {
            setFieldValue(notification, "createdAt", createdAt);
        }

        return notification;
    }

    /**
     * 여러 개의 테스트 알림 생성
     * @param count 생성할 알림 수
     * @param receiver 수신자
     * @return Notification 리스트
     */
    public static List<Notification> createNotifications(int count, User receiver) {
        List<Notification> notifications = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            notifications.add(aNotification()
                    .withReceiver(receiver)
                    .withType(NotificationType.COMMENT)
                    .withMessage("Test notification " + i)
                    .withRelatedId((long) i)
                    .createdDaysAgo(count - i) // 최신 것부터 오래된 순
                    .build());
        }
        return notifications;
    }

    /**
     * 다양한 타입의 알림 생성
     * @param receiver 수신자
     * @return 다양한 타입의 Notification 리스트
     */
    public static List<Notification> createMixedNotifications(User receiver) {
        List<Notification> notifications = new ArrayList<>();
        
        notifications.add(aCommentNotification(receiver, 1L).build());
        notifications.add(aReplyNotification(receiver, 2L).build());
        notifications.add(aLikeNotification(receiver, 3L).build());
        notifications.add(aPaperMessageNotification(receiver).build());
        notifications.add(anAdminNotification(receiver, "시스템 점검 안내").build());
        
        return notifications;
    }

    /**
     * 읽은/읽지 않은 알림 혼합 생성
     * @param receiver 수신자
     * @param unreadCount 읽지 않은 알림 수
     * @param readCount 읽은 알림 수
     * @return Notification 리스트
     */
    public static List<Notification> createMixedReadStatus(User receiver, int unreadCount, int readCount) {
        List<Notification> notifications = new ArrayList<>();
        
        // 읽지 않은 알림
        for (int i = 1; i <= unreadCount; i++) {
            notifications.add(aNotification()
                    .withReceiver(receiver)
                    .withMessage("Unread notification " + i)
                    .asUnread()
                    .build());
        }
        
        // 읽은 알림
        for (int i = 1; i <= readCount; i++) {
            notifications.add(aNotification()
                    .withReceiver(receiver)
                    .withMessage("Read notification " + i)
                    .asRead()
                    .build());
        }
        
        return notifications;
    }

    /**
     * 리플렉션을 통한 private 필드 값 설정
     */
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }
}