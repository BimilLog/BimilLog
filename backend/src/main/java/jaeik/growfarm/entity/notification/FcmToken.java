package jaeik.growfarm.entity.notification;

import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @NotNull
    @Column(nullable = false)
    private String fcmRegistrationToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    public void updateToken(String token) {
        this.fcmRegistrationToken = token;
    }

}
