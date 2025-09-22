package jaeik.bimillog.domain.user.entity;

import jaeik.bimillog.infrastructure.adapter.in.auth.dto.UserInfoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * <h2>기존 사용자 상세 정보</h2>
 * <p>소셜 로그인 시 기존 회원의 상세 정보를 담는 엔티티입니다.</p>
 * <p>사용자의 모든 정보(ID, 프로필, 토큰, 권한)를 포함하며 JWT 쿠키 생성에 사용됩니다.</p>
 * <p>클라이언트 응답용으로는 {@link UserInfoResponseDTO}를 사용해야 합니다.</p>
 *
 * <h3>사용 시나리오:</h3>
 * <ul>
 *   <li>기존 회원 소셜 로그인 시 생성</li>
 *   <li>JWT 쿠키 생성에 필요한 정보 제공</li>
 *   <li>보안 필터에서 사용자 인증 정보 관리</li>
 * </ul>
 *
 * @author jaeik
 * @version 2.0.0
 * @see UserDetail
 * @see NewUserDetail
 * @see UserInfoResponseDTO 클라이언트 응답용 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExistingUserDetail implements UserDetail{

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

    public static ExistingUserDetail of(User user, Long tokenId, @Nullable Long fcmTokenId) {
        Long settingId = (user.getSetting() != null) ? user.getSetting().getId() : null;
        
        return ExistingUserDetail.builder()
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
