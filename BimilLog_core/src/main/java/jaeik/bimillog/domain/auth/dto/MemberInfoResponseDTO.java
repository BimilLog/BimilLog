package jaeik.bimillog.domain.auth.dto;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.MemberRole;
import lombok.Builder;

/**
 * <h2>사용자 정보 조회 API 응답 DTO</h2>
 * <p>GET /api/auth/me API의 응답으로 사용되는 DTO입니다.</p>
 * <p>클라이언트에게 노출되어도 안전한 사용자 정보만을 포함합니다.</p>
 *
 * @author Jaeik
 * @version 2.3.0
 */
@Builder
public record MemberInfoResponseDTO(Long memberId, Long settingId, String socialNickname, String thumbnailImage,
                                    String memberName, MemberRole role) {

    /**
     * <h3>CustomUserDetails에서 MemberInfoResponseDTO로 변환</h3>
     * <p>사용자 상세 정보에서 클라이언트 응답용 DTO를 생성합니다.</p>
     *
     * @param userDetails CustomUserDetails 객체
     * @return 변환된 MemberInfoResponseDTO 객체
     * @since 2.3.0
     * @author Jaeik
     */
    public static MemberInfoResponseDTO from(CustomUserDetails userDetails) {
        return MemberInfoResponseDTO.builder()
                .memberId(userDetails.getMemberId())
                .settingId(userDetails.getSettingId())
                .socialNickname(userDetails.getSocialNickname())
                .thumbnailImage(userDetails.getThumbnailImage())
                .memberName(userDetails.getMemberName())
                .role(userDetails.getRole())
                .build();
    }
}
