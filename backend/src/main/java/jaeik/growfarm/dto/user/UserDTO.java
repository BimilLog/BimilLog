package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import lombok.Getter;
import lombok.Setter;

/**
 * <h3>사용자 정보 DTO</h3>
 * <p>
 * 사용자의 기본 정보를 담는 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Setter
@Getter
public class UserDTO {

    private Long userId;

    private Long kakaoId;

    private SettingDTO settingDTO;

    private String kakaoNickname;

    private String thumbnailImage;

    private String farmName;

    private UserRole role;

    public UserDTO (Users user) {
        this.userId = user.getId();
        this.kakaoId = user.getKakaoId();
        this.settingDTO = new SettingDTO(user.getSetting());
        this.kakaoNickname = user.getKakaoNickname();
        this.thumbnailImage = user.getThumbnailImage();
        this.farmName = user.getUserName();
        this.role = user.getRole();
    }

    public UserDTO (Users user, SettingDTO settingDTO) {
        this.userId = user.getId();
        this.kakaoId = user.getKakaoId();
        this.settingDTO = settingDTO;
        this.kakaoNickname = user.getKakaoNickname();
        this.thumbnailImage = user.getThumbnailImage();
        this.farmName = user.getUserName();
        this.role = user.getRole();
    }

    public UserDTO(Long userId, Long kakaoId, String kakaoNickname, String thumbnailImage, String farmName, UserRole role, SettingDTO settingDTO) {
        this.userId = userId;
        this.kakaoId = kakaoId;
        this.kakaoNickname = kakaoNickname;
        this.thumbnailImage = thumbnailImage;
        this.farmName = farmName;
        this.role = role;
        this.settingDTO = settingDTO;
    }
}