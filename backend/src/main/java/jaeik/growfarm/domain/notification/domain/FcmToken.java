package jaeik.growfarm.domain.notification.domain;

import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>FCM 토큰 엔티티</h2>
 * <p>Firebase Cloud Messaging(FCM) 토큰 정보를 저장하는 엔티티</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Entity
@SuperBuilder
@NoArgsConstructor
@Getter
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "fcm_token_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false)
    private String fcmRegistrationToken;

    public static FcmToken create(User user, String token) {
        return FcmToken.builder()
                .user(user)
                .fcmRegistrationToken(token)
                .build();
    }
}

