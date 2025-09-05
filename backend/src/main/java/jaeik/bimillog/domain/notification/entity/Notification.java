package jaeik.bimillog.domain.notification.entity;

import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>알림 엔티티</h2>
 * <p>사용자에게 전송되는 알림 정보를 저장하는 엔티티</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User users;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private boolean isRead;

    /**
     * <h3>알림 생성 팩토리 메소드</h3>
     * <p>새로운 알림 엔티티를 생성합니다.</p>
     *
     * @param user 알림을 받을 사용자 엔티티
     * @param notificationType 알림 유형
     * @param content 알림 내용
     * @param url 알림 클릭 시 이동할 URL
     * @return 생성된 Notification 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Notification create(User user, NotificationType notificationType, String content, String url) {
        return Notification.builder()
                .users(user)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .isRead(false)
                .build();
    }
}

