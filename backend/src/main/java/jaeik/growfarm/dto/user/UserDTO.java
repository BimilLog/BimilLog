package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.SocialProvider;
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

    private String socialId;

    private SocialProvider provider;

    private Long settingId;

    private String socialNickname;

    private String thumbnailImage;

    @Size(max = 8, message = "닉네임 은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    private UserRole role;

    public UserDTO (Users user) {
        this.userId = user.getId();
        this.socialId = user.getSocialId();
        this.provider = user.getProvider();
        this.settingId = user.getSetting().getId();
        this.socialNickname = user.getSocialNickname();
        this.thumbnailImage = user.getThumbnailImage();
        this.userName = user.getUserName();
        this.role = user.getRole();
    }

    public UserDTO(Long userId, String socialId, SocialProvider provider, String socialNickname, String thumbnailImage, String userName, UserRole role, Long settingId) {
        this.userId = userId;
        this.socialId = socialId;
        this.provider = provider;
        this.socialNickname = socialNickname;
        this.thumbnailImage = thumbnailImage;
        this.userName = userName;
        this.role = role;
        this.settingId = settingId;
    }
}