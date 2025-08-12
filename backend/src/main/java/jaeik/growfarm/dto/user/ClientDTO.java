package jaeik.growfarm.dto.user;

import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.domain.UserRole;
import org.springframework.lang.Nullable;
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
    
    // FCM 토큰 ID는 이벤트 기반 방식으로 변경되어 선택적으로 사용됨
    @Nullable
    private Long fcmTokenId;

    public static ClientDTO of(User user, Long tokenId, @Nullable Long fcmTokenId) {
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
