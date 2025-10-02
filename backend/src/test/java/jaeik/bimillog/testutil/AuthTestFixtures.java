package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;

/**
 * <h2>인증 도메인 테스트 유틸리티</h2>
 * <p>소셜/토큰 기반 테스트에서 공통으로 사용하는 상수 및 헬퍼 메서드를 제공합니다.</p>
 */
public final class AuthTestFixtures {

    public static final String TEST_SOCIAL_ID = "kakao123456";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_SOCIAL_NICKNAME = "테스트회원";
    public static final String TEST_PROFILE_IMAGE = "http://example.com/profile.jpg";
    public static final String TEST_ACCESS_TOKEN = "access-test-token";
    public static final String TEST_REFRESH_TOKEN = "refresh-test-token";
    public static final String TEST_AUTH_CODE = "auth-code-123";
    public static final String TEST_FCM_TOKEN = "fcm-token-123";
    public static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;

    public static MemberDetail createExistingMemberDetail(Member member) {
        return createExistingMemberDetail(member, null, null);
    }

    public static MemberDetail createExistingMemberDetail(Member member, Long tokenId, Long fcmTokenId) {
        Long settingId = 1L;
        if (member.getSetting() != null && member.getSetting().getId() != null) {
            settingId = member.getSetting().getId();
        }

        return MemberDetail.builder()
                .memberId(member.getId() != null ? member.getId() : 1L)
                .settingId(settingId)
                .socialId(member.getSocialId())
                .socialNickname(member.getSocialNickname())
                .thumbnailImage(member.getThumbnailImage())
                .memberName(member.getMemberName())
                .provider(member.getProvider())
                .role(member.getRole())
                .tokenId(tokenId)
                .fcmTokenId(fcmTokenId)
                .build();
    }

    public static CustomUserDetails createCustomUserDetails(Member member) {
        return new CustomUserDetails(createExistingMemberDetail(member));
    }

    public static CustomUserDetails createCustomUserDetails(Member member, Long tokenId, Long fcmTokenId) {
        return new CustomUserDetails(createExistingMemberDetail(member, tokenId, fcmTokenId));
    }

}
