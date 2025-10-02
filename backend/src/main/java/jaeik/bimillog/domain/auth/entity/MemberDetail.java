package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.MemberRole;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.MemberInfoResponseDTO;
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

    // 사용자 기본 정보
    @Nullable
    private Long memberId;

    @Nullable
    private String socialId;

    @Nullable
    private SocialProvider provider;

    @Nullable
    private Long settingId;

    @Nullable
    private String socialNickname;

    @Nullable
    private String thumbnailImage;

    @Nullable
    private String memberName;

    @Nullable
    private MemberRole role;

    // 인증 관련 추가 필드
    @Nullable
    private Long authTokenId;

    @Nullable
    private Long fcmTokenId;

    /**
     * 신규 회원 임시 식별자
     * <p>신규 회원인 경우에만 값이 존재하며, 기존 회원은 null입니다.</p>
     * <p>Redis에 저장된 임시 데이터 조회 키로 사용됩니다.</p>
     */
    @Nullable
    private String uuid;

    /**
     * <h3>기존 회원용 팩토리 메서드</h3>
     * <p>기존 회원의 상세 정보를 생성합니다. uuid는 null로 설정됩니다.</p>
     *
     * @param member Member 엔티티
     * @param authTokenId AuthToken ID
     * @param fcmTokenId FCM 토큰 ID (nullable)
     * @return MemberDetail 객체
     */
    public static MemberDetail ofExisting(Member member, Long authTokenId, @Nullable Long fcmTokenId) {
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
                .fcmTokenId(fcmTokenId)
                .uuid(null)
                .build();
    }

    /**
     * <h3>신규 회원용 팩토리 메서드</h3>
     * <p>신규 회원의 임시 정보를 생성합니다. uuid만 설정되고 나머지는 null입니다.</p>
     *
     * @param uuid 임시 사용자 식별자
     * @return MemberDetail 객체
     */
    public static MemberDetail ofNew(String uuid) {
        return MemberDetail.builder()
                .uuid(uuid)
                .build();
    }
}
