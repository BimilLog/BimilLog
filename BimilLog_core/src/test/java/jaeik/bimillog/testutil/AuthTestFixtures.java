package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;

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
    public static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;

    public static CustomUserDetails createCustomUserDetails(Member member) {
        return CustomUserDetails.ofExisting(member, 1L);
    }

    public static CustomUserDetails createCustomUserDetails(Member member, Long tokenId) {
        return CustomUserDetails.ofExisting(member, tokenId != null ? tokenId : 1L);
    }

}
