package jaeik.growfarm.dto.user;

import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>클라이언트 DTO</h2>
 * <p>사용자 정보를 포함하는 데이터 전송 객체</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Setter
@Getter
public class ClientDTO extends UserDTO{

    private Long tokenId;
    private Long fcmTokenId;

    public ClientDTO(Users user, Long tokenId, Long fcmTokenId) {
        super(user);
        this.tokenId = tokenId;
        this.fcmTokenId = fcmTokenId;
    }

    public ClientDTO(Users user, SettingDTO settingDTO, Long tokenId, Long fcmTokenId) {
        super(user, settingDTO);
        this.tokenId = tokenId;
        this.fcmTokenId = fcmTokenId;
    }

    public ClientDTO(Long userId, Long kakaoId, String kakaoNickname, String thumbnailImage, String farmName, UserRole role, Long tokenId, Long fcmTokenId, SettingDTO settingDTO) {
        super(userId, kakaoId, kakaoNickname, thumbnailImage, farmName, role, settingDTO);
        this.tokenId = tokenId;
        this.fcmTokenId = fcmTokenId;
    }
}
