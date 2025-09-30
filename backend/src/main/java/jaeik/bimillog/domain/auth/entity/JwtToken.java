package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.user.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * <h2>JWT 토큰 엔티티</h2>
 * <p>사용자의 JWT 리프레시 토큰 정보를 저장하는 엔티티</p>
 * <p>Token Rotation과 재사용 공격 감지를 위한 사용 이력을 추적합니다</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@SuperBuilder
@NoArgsConstructor
@Getter
@Table(name = "jwt_token")
public class JwtToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jwt_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User users;

    @Column(name = "jwt_refresh_token", length = 500)
    private String jwtRefreshToken;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "use_count", columnDefinition = "INT DEFAULT 0")
    private Integer useCount;

    /**
     * <h3>JWT 토큰 생성</h3>
     * <p>사용자와 JWT 리프레시 토큰으로 JwtToken 엔티티를 생성합니다.</p>
     * <p>사용 횟수는 0으로 초기화됩니다.</p>
     *
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @param user 사용자 엔티티
     * @return 생성된 JwtToken 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static JwtToken createToken(String jwtRefreshToken, User user) {
        return JwtToken.builder()
                .jwtRefreshToken(jwtRefreshToken)
                .users(user)
                .useCount(0)
                .build();
    }

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     * <p>JwtToken Rotation 시 JWT 리프레시 토큰을 새로운 값으로 갱신합니다.</p>
     * <p>사용 횟수를 0으로 초기화하고 마지막 사용 시각을 현재 시각으로 업데이트합니다.</p>
     *
     * @param newJwtRefreshToken 새로운 JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateJwtRefreshToken(String newJwtRefreshToken) {
        this.jwtRefreshToken = newJwtRefreshToken;
        this.lastUsedAt = LocalDateTime.now();
        this.useCount = 0;
    }

    /**
     * <h3>토큰 사용 기록</h3>
     * <p>리프레시 토큰이 사용될 때마다 호출되어 사용 이력을 기록합니다.</p>
     * <p>재사용 공격 감지를 위해 사용 횟수를 증가시킵니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.useCount = (this.useCount == null ? 0 : this.useCount) + 1;
    }
}
