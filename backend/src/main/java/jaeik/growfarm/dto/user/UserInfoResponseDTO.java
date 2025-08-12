package jaeik.growfarm.dto.user;

import jaeik.growfarm.global.domain.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * <h2>사용자 정보 조회 API 응답 DTO</h2>
 * <p>GET /api/auth/me API의 응답으로 사용되는 DTO입니다.</p>
 * <p>클라이언트에게 노출되어도 안전한 사용자 정보만을 포함합니다.</p>
 *
 * @author BimilLog
 * @version 1.0.0
 */
@Getter
@Builder
public class UserInfoResponseDTO {

    private final Long userId;
    private final Long settingId;
    private final String socialNickname;
    private final String thumbnailImage;
    private final String userName;
    private final UserRole role;

    /**
     * <h3>ClientDTO에서 UserInfoResponseDTO로 변환</h3>
     * <p>내부용 DTO인 ClientDTO에서 클라이언트 응답용 DTO를 생성합니다.</p>
     *
     * @param clientDTO 원본 ClientDTO 객체
     * @return 변환된 UserInfoResponseDTO 객체
     */
    public static UserInfoResponseDTO from(ClientDTO clientDTO) {
        return UserInfoResponseDTO.builder()
                .userId(clientDTO.getUserId())
                .settingId(clientDTO.getSettingId())
                .socialNickname(clientDTO.getSocialNickname())
                .thumbnailImage(clientDTO.getThumbnailImage())
                .userName(clientDTO.getUserName())
                .role(clientDTO.getRole())
                .build();
    }
}
