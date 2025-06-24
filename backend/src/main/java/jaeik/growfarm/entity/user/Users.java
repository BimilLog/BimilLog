package jaeik.growfarm.entity.user;

import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.UserDTO;
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
 * @since 1.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_user_username", columnList = "userName"),
        @Index(name = "idx_user_kakao_id_username", columnList = "kakao_id, userName"),
})
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
    @Column(name = "user_name", unique = true, nullable = false)
    private String userName;

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
     * @param kakaoNickname  카카오 닉네임
     * @param thumbnailImage 프로필 이미지 URL
     * @author Jaeik
     * @since 1.0.0
     */
    public void updateUserInfo(String kakaoNickname, String thumbnailImage) {
        this.kakaoNickname = kakaoNickname;
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
     * @since 1.0.0
     */
    public void updateUserName(String userName) {
        this.userName = userName;
    }

    public static Users createUser(KakaoInfoDTO kakaoInfoDTO, String userName, Setting setting) {
        return Users.builder()
                .kakaoId(kakaoInfoDTO.getKakaoId())
                .kakaoNickname(kakaoInfoDTO.getKakaoNickname())
                .thumbnailImage(kakaoInfoDTO.getThumbnailImage())
                .userName(userName)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }

    public static Users createUser(UserDTO userDTO) {
        return Users.builder()
                .id(userDTO.getUserId())
                .kakaoId(userDTO.getKakaoId())
                .userName(userDTO.getUserName())
                .role(userDTO.getRole())
                .kakaoNickname(userDTO.getKakaoNickname())
                .thumbnailImage(userDTO.getThumbnailImage())
                .setting(Setting.createSetting(userDTO.getSettingDTO()))
                .build();
    }
}
