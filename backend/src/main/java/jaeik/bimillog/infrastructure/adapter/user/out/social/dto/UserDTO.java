package jaeik.bimillog.infrastructure.adapter.user.out.social.dto;

import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.auth.in.web.dto.UserInfoResponseDTO;
import org.springframework.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>클라이언트 인증 정보 DTO</h2>
 * <p>인증 시스템에서 사용하는 완전한 사용자 정보를 담는 내부용 DTO입니다.</p>
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
public class UserDTO {

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
    
    // FCM 토큰 ID는 이벤트 기반 방식으로 변경되어 선택적으로 사용됨
    @Nullable
    private Long fcmTokenId;

    // 이벤트 기반 Setting 생성 흐름: User.createUser() → save() → UserSignedUpEvent → Setting 생성
    // 따라서 UserDTO.of() 호출 시점에서는 Setting이 null일 수 있음 (정상적인 상황)
    // 이벤트 기반 비동기 처리로 인한 일시적 null 상태를 고려한 방어적 프로그래밍
    public static UserDTO of(User user, Long tokenId, @Nullable Long fcmTokenId) {
        // Setting이 null인 경우에 대한 방어 코드 (이벤트 처리 전 상태 고려)
        Long settingId = (user.getSetting() != null) ? user.getSetting().getId() : null;
        
        return UserDTO.builder()
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
