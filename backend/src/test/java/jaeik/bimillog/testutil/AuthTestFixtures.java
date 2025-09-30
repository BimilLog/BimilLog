package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.domain.user.entity.userdetail.ExistingUserDetail;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;

/**
 * <h2>인증 도메인 테스트 유틸리티</h2>
 * <p>소셜/토큰 기반 테스트에서 공통으로 사용하는 상수 및 헬퍼 메서드를 제공합니다.</p>
 */
public final class AuthTestFixtures {

    public static final String TEST_SOCIAL_ID = "kakao123456";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_SOCIAL_NICKNAME = "테스트유저";
    public static final String TEST_PROFILE_IMAGE = "http://example.com/profile.jpg";
    public static final String TEST_ACCESS_TOKEN = "access-test-token";
    public static final String TEST_REFRESH_TOKEN = "refresh-test-token";
    public static final String TEST_AUTH_CODE = "auth-code-123";
    public static final String TEST_FCM_TOKEN = "fcm-token-123";
    public static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;

    public static ExistingUserDetail createExistingUserDetail(User user) {
        return createExistingUserDetail(user, null, null);
    }

    public static ExistingUserDetail createExistingUserDetail(User user, Long tokenId, Long fcmTokenId) {
        Long settingId = 1L;
        if (user.getSetting() != null && user.getSetting().getId() != null) {
            settingId = user.getSetting().getId();
        }

        return ExistingUserDetail.builder()
                .userId(user.getId() != null ? user.getId() : 1L)
                .settingId(settingId)
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .tokenId(tokenId)
                .fcmTokenId(fcmTokenId)
                .build();
    }

    public static CustomUserDetails createCustomUserDetails(User user) {
        return new CustomUserDetails(createExistingUserDetail(user));
    }

    public static CustomUserDetails createCustomUserDetails(User user, Long tokenId, Long fcmTokenId) {
        return new CustomUserDetails(createExistingUserDetail(user, tokenId, fcmTokenId));
    }

}
