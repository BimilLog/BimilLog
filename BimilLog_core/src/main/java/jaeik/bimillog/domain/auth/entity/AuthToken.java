package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "auth_token")
public class AuthToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    private Long id;

    // null + 허용, FK 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @NotNull
    @Column(name = "refresh_token")
    private String refreshToken;

    private LocalDateTime lastUsedAt;

    @Column(name = "use_count", columnDefinition = "INT DEFAULT 0")
    private Integer useCount;

    /**
     * <h3>JWT 토큰 생성</h3>
     * <p>사용자와 JWT 리프레시 토큰으로 AuthToken 엔티티를 생성합니다.</p>
     * <p>사용 횟수는 0으로 초기화됩니다.</p>
     *
     * @param jwtRefreshToken JWT 리프레시 토큰
     * @param member 사용자 엔티티
     * @return 생성된 AuthToken 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static AuthToken createToken(String jwtRefreshToken, Member member) {
        return AuthToken.builder()
                .refreshToken(jwtRefreshToken)
                .member(member)
                .useCount(0)
                .build();
    }

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     * <p>AuthToken Rotation 시 JWT 리프레시 토큰을 새로운 값으로 갱신합니다.</p>
     * <p>사용 횟수를 0으로 초기화하고 마지막 사용 시각을 현재 시각으로 업데이트합니다.</p>
     *
     * @param newJwtRefreshToken 새로운 JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateJwtRefreshToken(String newJwtRefreshToken) {
        this.refreshToken = newJwtRefreshToken;
        this.lastUsedAt = LocalDateTime.now();
        this.useCount = 0;
    }
}
