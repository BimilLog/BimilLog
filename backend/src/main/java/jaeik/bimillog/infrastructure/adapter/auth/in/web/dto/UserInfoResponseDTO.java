package jaeik.bimillog.infrastructure.adapter.auth.in.web.dto;

import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import lombok.Builder;

/**
 * <h2>사용자 정보 조회 API 응답 DTO</h2>
 * <p>GET /api/auth/me API의 응답으로 사용되는 DTO입니다.</p>
 * <p>클라이언트에게 노출되어도 안전한 사용자 정보만을 포함합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Builder
public record UserInfoResponseDTO(Long userId, Long settingId, String socialNickname, String thumbnailImage,
                                  String userName, UserRole role) {

    /**
     * <h3>ClientDTO에서 UserInfoResponseDTO로 변환</h3>
     * <p>내부용 DTO인 ClientDTO에서 클라이언트 응답용 DTO를 생성합니다.</p>
     *
     * @param userDTO 원본 ClientDTO 객체
     * @return 변환된 UserInfoResponseDTO 객체
     * @since 2.0.0
     * @author jaeik
     */
    public static UserInfoResponseDTO from(UserDTO userDTO) {
        return UserInfoResponseDTO.builder()
                .userId(userDTO.getUserId())
                .settingId(userDTO.getSettingId())
                .socialNickname(userDTO.getSocialNickname())
                .thumbnailImage(userDTO.getThumbnailImage())
                .userName(userDTO.getUserName())
                .role(userDTO.getRole())
                .build();
    }
}
