package jaeik.bimillog.domain.user.entity;

import jaeik.bimillog.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>토큰 엔티티</h2>
 * <p>사용자의 카카오 및 JWT 토큰 정보를 저장하는 엔티티</p>
 * <p>카카오 액세스 토큰, 카카오 리프레시 토큰, JWT 리프레시 토큰을 포함</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@SuperBuilder
@NoArgsConstructor
@Getter
@Table(name = "token")
public class Token extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User users;

    private String accessToken;

    private String refreshToken;

    /**
     * <h3>카카오 토큰 업데이트</h3>
     * <p>카카오 액세스 토큰과 리프레시 토큰을 업데이트합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param refreshToken 카카오 리프레시 토큰 (null 또는 빈 문자열인 경우 업데이트하지 않음)
     */
    public void updateToken(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
    }

    /**
     * <h3>토큰 생성</h3>
     * <p>사용자와 토큰 정보로 Token 엔티티를 생성합니다.</p>
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param user 사용자 엔티티
     * @return 생성된 Token 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Token createToken(String accessToken, String refreshToken, User user) {
        return Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .users(user)
                .build();
    }

    /**
     * <h3>임시 토큰 생성</h3>
     * <p>사용자 없이 토큰 정보만으로 Token 엔티티를 생성합니다.</p>
     * <p>소셜 로그인 중간 단계에서 사용됩니다.</p>
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return 생성된 Token 엔티티 (사용자 없음)
     * @author Jaeik
     * @since 2.0.0
     */
    public static Token createTemporaryToken(String accessToken, String refreshToken) {
        return Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
}
