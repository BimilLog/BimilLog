package jaeik.growfarm.dto.user;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.UserRole;
import jaeik.growfarm.domain.user.domain.User;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>클라이언트 DTO</h2>
 * <p>사용자 정보를 포함하는 데이터 전송 객체</p>
 *
 * @author Jaeik
 * @version  2.0.0
 */
@Setter
@Getter
public class ClientDTO extends UserDTO{

    private Long tokenId;
    private Long fcmTokenId;

    public ClientDTO(User user, Long tokenId, Long fcmTokenId) {
        super(user);
        this.tokenId = tokenId;
        this.fcmTokenId = fcmTokenId;
    }

    public ClientDTO(Long userId, String socialId, SocialProvider provider, String socialNickname, String thumbnailImage, String userName, UserRole role, Long tokenId, Long fcmTokenId, Long settingId) {
        super(userId, socialId, provider, socialNickname, thumbnailImage, userName, role, settingId);
        this.tokenId = tokenId;
        this.fcmTokenId = fcmTokenId;
    }
}
