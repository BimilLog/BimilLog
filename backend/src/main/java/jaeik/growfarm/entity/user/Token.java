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

// 토큰 엔티티
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
