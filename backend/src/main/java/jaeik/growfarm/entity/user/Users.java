package jaeik.growfarm.entity.user;

import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "user_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "token_id", unique = true)
    private Token token;

    @NotNull
    @OneToOne
    @JoinColumn(name = "setting_id", unique = true, nullable = false)
    private Setting setting;

    @NotNull
    @Column(name = "kakao_id", unique = true, nullable = false) // 카카오 회원 번호
    private Long kakaoId;

    @NotNull
    @Column(nullable = false) // 카카오 닉네임
    private String kakaoNickname;

    @NotNull
    @Column(nullable = false) // 카카오 프사 작은 이미지
    private String thumbnailImage;

    @NotNull
    @Column(unique = true, nullable = false) // 농장 이름
    private String farmName;

    @NotNull
    @Enumerated(value = EnumType.STRING) // 회원 등급
    @Column(nullable = false)
    private UserRole role;

    public void deleteTokenId() {
        this.token = null;
    }

    public void updateUserInfo(Token token, String kakaoNickname, String thumbnailImage) {
        this.token = token;
        this.kakaoNickname = kakaoNickname;
        this.thumbnailImage = thumbnailImage;
    }

    // 농장 이름 변경
    public void updateFarmName(String farmName) {
        this.farmName = farmName;
    }
}
