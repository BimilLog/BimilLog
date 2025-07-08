package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * <h3>사용자 정보 DTO</h3>
 * <p>
 * 사용자의 기본 정보를 담는 데이터 전송 객체
 * </p>
 * 
 * @version  1.0.0
 * @author Jaeik
 */
@Setter
@Getter
public class UserDTO {

    private Long userId;

    private Long kakaoId;

    private Long settingId;

    private String kakaoNickname;

    private String thumbnailImage;

    @Size(max = 8, message = "닉네임 은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    private UserRole role;

    public UserDTO (Users user) {
        this.userId = user.getId();
        this.kakaoId = user.getKakaoId();
        this.settingId = user.getSetting().getId();
        this.kakaoNickname = user.getKakaoNickname();
        this.thumbnailImage = user.getThumbnailImage();
        this.userName = user.getUserName();
        this.role = user.getRole();
    }

    public UserDTO (Users user, SettingDTO settingDTO) {
        this.userId = user.getId();
        this.kakaoId = user.getKakaoId();
        this.settingId = settingDTO.getSettingId();
        this.kakaoNickname = user.getKakaoNickname();
        this.thumbnailImage = user.getThumbnailImage();
        this.userName = user.getUserName();
        this.role = user.getRole();
    }

    public UserDTO(Long userId, Long kakaoId, String kakaoNickname, String thumbnailImage, String userName, UserRole role, Long settingId) {
        this.userId = userId;
        this.kakaoId = kakaoId;
        this.kakaoNickname = kakaoNickname;
        this.thumbnailImage = thumbnailImage;
        this.userName = userName;
        this.role = role;
        this.settingId = settingId;
    }
}