package jaeik.bimillog.domain.global.entity;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberRole;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.auth.dto.MemberInfoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * <h2>사용자 상세 정보</h2>
 * <p>소셜 로그인 시 사용자의 상세 정보를 담는 엔티티입니다.</p>
 * <p>기존 회원과 신규 회원을 모두 표현하며, uuid 필드로 구분합니다.</p>
 * <p>클라이언트 응답용으로는 {@link MemberInfoResponseDTO}를 사용해야 합니다.</p>
 *
 * <h3>사용 시나리오:</h3>
 * <ul>
 *   <li>기존 회원: JWT 쿠키 생성에 필요한 정보 제공 (uuid = null)</li>
 *   <li>신규 회원: 회원가입용 임시 UUID 제공 (uuid != null)</li>
 *   <li>보안 필터에서 사용자 인증 정보 관리</li>
 * </ul>
 *
 * @author jaeik
 * @version 2.0.0
 * @see MemberInfoResponseDTO 클라이언트 응답용 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDetail {

    private Long memberId;

    private String socialId;

    private SocialProvider provider;

    private Long settingId;

    private String socialNickname;

    private String thumbnailImage;

    private String memberName;

    private MemberRole role;

    private Long authTokenId;

    @Nullable
    private Long fcmTokenId;


    /**
     * <h3>기존 회원용 팩토리 메서드</h3>
     * <p>기존 회원의 상세 정보를 생성합니다. uuid는 null로 설정됩니다.</p>
     *
     * @param member Member 엔티티
     * @param authTokenId AuthToken ID
     * @return MemberDetail 객체
     */
    public static MemberDetail ofExisting(Member member, Long authTokenId) {
        Long settingId = (member.getSetting() != null) ? member.getSetting().getId() : null;

        return MemberDetail.builder()
                .memberId(member.getId())
                .socialId(member.getSocialId())
                .provider(member.getProvider())
                .settingId(settingId)
                .socialNickname(member.getSocialNickname())
                .thumbnailImage(member.getThumbnailImage())
                .memberName(member.getMemberName())
                .role(member.getRole())
                .authTokenId(authTokenId)
                .build();
    }
}
