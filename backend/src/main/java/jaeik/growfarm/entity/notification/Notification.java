package jaeik.growfarm.entity.notification;

import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


/**
 * <h2>알림 엔티티</h2>
 * <p>
 * 알림을 저장하는 엔티티입니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "notification_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @NotNull
    @Column(nullable = false)
    private String data;

    private String url;

    @NotNull
    @Column(nullable = false)
    private boolean isRead;


    public void markAsRead(){
        isRead=true;
    }

    /**
     * <h3>알림 생성</h3>
     * <p>
     * 알림 객체를 생성합니다.
     * </p>
     *
     * @param user            사용자 정보
     * @param notificationType 알림 유형
     * @param data            알림 데이터
     * @param url             알림 URL
     * @return 생성된 Notification 객체
     * @author Jaeik
     * @since 1.0.0
     */
    public static Notification createNotification(Users user, NotificationType notificationType, String data, String url) {
        return Notification.builder()
                .users(user)
                .notificationType(notificationType)
                .data(data)
                .url(url)
                .isRead(false)
                .build();
    }
}
