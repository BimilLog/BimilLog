package jaeik.bimillog.domain.notification.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.user.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>FCM 토큰 엔티티</h2>
 * <p>Firebase Cloud Messaging(FCM) 토큰 정보를 저장하는 엔티티입니다.</p>
 * <p>푸시 알림 발송을 위한 사용자별 FCM 토큰 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false)
    private String fcmRegistrationToken;

    /**
     * <h3>FCM 토큰 생성 팩토리 메소드</h3>
     * <p>새로운 FCM 토큰 엔티티를 생성합니다.</p>
     * <p>사용자와 FCM 토큰을 연결하여 푸시 알림 발송 준비</p>
     *
     * @param user 토큰을 소유할 사용자 엔티티
     * @param token FCM 등록 토큰 문자열
     * @return 생성된 FcmToken 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static FcmToken create(User user, String token) {
        return FcmToken.builder()
                .user(user)
                .fcmRegistrationToken(token)
                .build();
    }
}

