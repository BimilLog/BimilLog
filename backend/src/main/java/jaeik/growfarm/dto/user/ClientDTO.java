package jaeik.growfarm.dto.user;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.domain.user.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    // UserDTO 필드
    private Long userId;
    private String socialId;
    private SocialProvider provider;
    private Long settingId;
    private String socialNickname;
    private String thumbnailImage;
    private String userName;
    private UserRole role;

    // ClientDTO 추가 필드
    private Long tokenId;
    private Long fcmTokenId;

    public static ClientDTO of(User user, Long tokenId, Long fcmTokenId) {
        return ClientDTO.builder()
                .userId(user.getId())
                .socialId(user.getSocialId())
                .provider(user.getProvider())
                .settingId(user.getSetting().getId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .role(user.getRole())
                .tokenId(tokenId)
                .fcmTokenId(fcmTokenId)
                .build();
    }
}
