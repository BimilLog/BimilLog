package jaeik.bimillog.infrastructure.adapter.auth.dto;

import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.entity.UserDetail;
import lombok.Builder;

/**
 * <h2>사용자 정보 조회 API 응답 DTO</h2>
 * <p>GET /api/auth/me API의 응답으로 사용되는 DTO입니다.</p>
 * <p>클라이언트에게 노출되어도 안전한 사용자 정보만을 포함합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Builder
public record UserInfoResponseDTO(Long userId, Long settingId, String socialNickname, String thumbnailImage,
                                  String userName, UserRole role) {

    /**
     * <h3>ClientDTO에서 UserInfoResponseDTO로 변환</h3>
     * <p>내부용 DTO인 ClientDTO에서 클라이언트 응답용 DTO를 생성합니다.</p>
     *
     * @param userDetail 원본 ClientDTO 객체
     * @return 변환된 UserInfoResponseDTO 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static UserInfoResponseDTO from(UserDetail userDetail) {
        return UserInfoResponseDTO.builder()
                .userId(userDetail.getUserId())
                .settingId(userDetail.getSettingId())
                .socialNickname(userDetail.getSocialNickname())
                .thumbnailImage(userDetail.getThumbnailImage())
                .userName(userDetail.getUserName())
                .role(userDetail.getRole())
                .build();
    }
}
