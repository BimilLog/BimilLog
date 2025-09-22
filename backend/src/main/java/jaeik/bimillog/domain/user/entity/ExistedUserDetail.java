package jaeik.bimillog.domain.user.entity;

import jaeik.bimillog.infrastructure.adapter.in.auth.dto.UserInfoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * <h2>유저 정보</h2>
 * <p>인증 시스템에서 사용하는 완전한 사용자 정보를 담는 내부용 엔티티입니다.</p>
 * <p>JWT 토큰, 보안 필터, 인증 처리에서 사용되며 민감한 정보를 포함합니다.</p>
 * <p>클라이언트 응답용으로는 {@link UserInfoResponseDTO}를 사용해야 합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 * @see UserInfoResponseDTO 클라이언트 응답용 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExistedUserDetail implements UserDetail{

    // 사용자 기본 정보
    private Long userId;
    private String socialId;
    private SocialProvider provider;
    private Long settingId;
    private String socialNickname;
    private String thumbnailImage;
    private String userName;
    private UserRole role;

    // 인증 관련 추가 필드
    private Long tokenId;
    
    @Nullable
    private Long fcmTokenId;

    public static ExistedUserDetail of(User user, Long tokenId, @Nullable Long fcmTokenId) {
        Long settingId = (user.getSetting() != null) ? user.getSetting().getId() : null;
        
        return ExistedUserDetail.builder()
                .userId(user.getId())
                .socialId(user.getSocialId())
                .provider(user.getProvider())
                .settingId(settingId)
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .role(user.getRole())
                .tokenId(tokenId)
                .fcmTokenId(fcmTokenId)
                .build();
    }
}
