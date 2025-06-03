package jaeik.growfarm.entity.user;

import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>토큰 엔티티</h2>
 * <p>사용자의 카카오 및 JWT 토큰 정보를 저장하는 엔티티</p>
 * <p>카카오 액세스 토큰, 카카오 리프레시 토큰, JWT 리프레시 토큰을 포함</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Entity
@SuperBuilder
@NoArgsConstructor
@Getter
public class Token extends BaseEntity {

    @Id
    @Column(name = "token_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @NotNull
    @Column(nullable = false)
    private String kakaoAccessToken;

    @NotNull
    @Column(nullable = false)
    private String kakaoRefreshToken;

    private String jwtRefreshToken;

    @Transactional
    public void updateKakaoToken(String kakaoAccessToken, String kakaoRefreshToken) {
        this.kakaoAccessToken = kakaoAccessToken;
        if (kakaoRefreshToken != null && !kakaoRefreshToken.isBlank()) {
            this.kakaoRefreshToken = kakaoRefreshToken;
        }
    }

    public static Token createToken(TokenDTO tokenDTO, Users user) {
        return Token.builder()
                .users(user)
                .kakaoAccessToken(tokenDTO.getKakaoAccessToken())
                .kakaoRefreshToken(tokenDTO.getKakaoRefreshToken())
                .jwtRefreshToken(tokenDTO.getJwtRefreshToken())
                .build();
    }
}
