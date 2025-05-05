package jaeik.growfarm.entity.notification;

import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 알림 엔티티
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
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
}
