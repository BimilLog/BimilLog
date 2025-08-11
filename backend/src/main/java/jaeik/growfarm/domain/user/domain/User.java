package jaeik.growfarm.domain.user.domain;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>사용자 엔티티</h2>
 * <p>
 * 카카오 로그인을 통한 사용자 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 닉네임, 카카오 정보, 설정 정보 등을 포함
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "userName"),
        @Index(name = "uk_provider_social_id", columnList = "provider, social_id", unique = true),
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "user_id")
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id", unique = true, nullable = false)
    private Setting setting;

    @NotNull
    @Column(name = "social_id", nullable = false)
    private String socialId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @NotNull
    @Column(name = "user_name", unique = true, nullable = false)
    private String userName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false) // 권한
    private UserRole role;

    @Column(name = "social_nickname") // 소셜 프로필 닉네임
    private String socialNickname;

    @Column(name = "thumbnail_image") // 카카오 프로필 이미지
    private String thumbnailImage;

    /**
     * <h3>사용자 정보 업데이트</h3>
     *
     * <p>
     * 카카오 닉네임과 프로필 이미지를 업데이트한다.
     * </p>
     *
     * @param socialNickname  카카오 닉네임
     * @param thumbnailImage 프로필 이미지 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateUserInfo(String socialNickname, String thumbnailImage) {
        this.socialNickname = socialNickname;
        this.thumbnailImage = thumbnailImage;
    }

    /**
     * <h3>닉네임 수정</h3>
     *
     * <p>
     * 사용자의 닉네임을 수정한다.
     * </p>
     *
     * @param userName 새로운 닉네임
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateUserName(String userName) {
        this.userName = userName;
    }

    /**
     * <h3>사용자 생성</h3>
     *
     * <p>
     * 카카오 정보와 사용자 이름, 설정 정보를 기반으로 새로운 사용자 엔티티를 생성한다.
     * </p>
     *
     * @param userData 카카오 정보 DTO
     * @param userName     사용자 이름
     * @param setting      사용자 설정 정보
     * @return 생성된 사용자 엔티티
     */
    public static User createUser(SocialLoginUserData userData, String userName, Setting setting) {
        return User.builder()
                .socialId(userData.getSocialId())
                .provider(userData.getProvider())
                .socialNickname(userData.getNickname())
                .thumbnailImage(userData.getProfileImageUrl())
                .userName(userName)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }
}
