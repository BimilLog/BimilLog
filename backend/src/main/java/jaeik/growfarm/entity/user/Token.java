package jaeik.growfarm.entity.user;

import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// 토큰 엔티티
@Entity
@SuperBuilder
@NoArgsConstructor
@Getter
public class Token extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "token_id")
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String kakaoAccessToken;

    @NotNull
    @Column(nullable = false)
    private String kakaoRefreshToken;

    private String jwtRefreshToken;

    public void updateJwtRefreshToken(String jwtRefreshToken) {
        this.jwtRefreshToken = jwtRefreshToken;
    }

}
