package jaeik.growfarm.entity.user;

import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 엔티티</h2>
 * <p>
 * 카카오 로그인을 통한 사용자 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 농장 이름, 카카오 정보, 설정 정보 등을 포함
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "user_id")
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id", unique = true, nullable = false)
    private Setting setting;

    @NotNull
    @Column(name = "kakao_id", unique = true, nullable = false) // 카카오 회원 번호
    private Long kakaoId;

    @NotNull
    @Column(name = "farm_name", unique = true, nullable = false) // 농장 이름
    private String farmName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false) // 권한
    private UserRole role;

    @Column(name = "kakao_nickname") // 카카오 닉네임
    private String kakaoNickname;

    @Column(name = "thumbnail_image") // 카카오 프로필 이미지
    private String thumbnailImage;

    /**
     * <h3>사용자 정보 업데이트</h3>
     *
     * <p>
     * 카카오 닉네임과 프로필 이미지를 업데이트한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param kakaoNickname  카카오 닉네임
     * @param thumbnailImage 프로필 이미지 URL
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void updateUserInfo(String kakaoNickname, String thumbnailImage) {
        this.kakaoNickname = kakaoNickname;
        this.thumbnailImage = thumbnailImage;
    }

    /**
     * <h3>농장 이름 업데이트</h3>
     *
     * <p>
     * 사용자의 농장 이름을 변경한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param farmName 새로운 농장 이름
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateFarmName(String farmName) {
        this.farmName = farmName;
    }
}
