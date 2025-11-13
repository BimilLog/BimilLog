package jaeik.bimillog.testutil.config;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.out.GlobalSocialStrategyAdapter;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.strategy.SocialFriendStrategy;
import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;

/**
 * <h2>테스트용 소셜 로그인 설정</h2>
 * <p>통합 테스트에서 외부 소셜 연동을 대체하는 전략을 제공합니다.</p>
 */
@TestConfiguration
public class TestSocialLoginAdapterConfig {

    private static final TestSocialFriendStrategy TEST_FRIEND_STRATEGY = new TestSocialFriendStrategy();
    private static final TestSocialPlatformStrategy TEST_PLATFORM_STRATEGY = new TestSocialPlatformStrategy(TEST_FRIEND_STRATEGY);

    @Bean
    @Primary
    public GlobalSocialStrategyAdapter testGlobalSocialStrategyAdapter() {
        return new GlobalSocialStrategyAdapter(Collections.singletonList(TEST_PLATFORM_STRATEGY));
    }

    public static void setFriendConsentRequired(boolean required) {
        TEST_FRIEND_STRATEGY.setConsentRequired(required);
    }

    private static final class TestSocialPlatformStrategy extends SocialPlatformStrategy {

        TestSocialPlatformStrategy(TestSocialFriendStrategy friendStrategy) {
            super(SocialProvider.KAKAO, new TestSocialAuthStrategy(), friendStrategy);
        }
    }

    private static final class TestSocialAuthStrategy implements SocialAuthStrategy {

        @Override
        public SocialProvider getProvider() {
            return SocialProvider.KAKAO;
        }

        @Override
        public SocialMemberProfile getSocialToken(String code, String state) {
            // state parameter is ignored in tests
            String socialId;
            String accessToken = "dummy-access-token";
            String refreshToken = "dummy-refresh-token";

            if ("new-member-code".equals(code)) {
                socialId = "new-member-social-id";
            } else if ("existing-member-code".equals(code)) {
                socialId = "test-social-id-12345";
            } else {
                socialId = "test-social-id";
            }

            return SocialMemberProfile.of(
                socialId,
                null,
                SocialProvider.KAKAO,
                "Test Member",
                "https://example.com/profile.jpg",
                accessToken,
                refreshToken
            );
        }

        @Override
        public void unlink(String socialId, String accessToken) {
            // no-op
        }

        @Override
        public void logout(String accessToken) {
            // no-op
        }

        @Override
        public void forceLogout(String socialId) {
            // no-op
        }

        @Override
        public String refreshAccessToken(String refreshToken) throws Exception {
            return "refreshed-access-token";
        }
    }

    private static final class TestSocialFriendStrategy implements SocialFriendStrategy {

        private boolean consentRequired;

        void setConsentRequired(boolean consentRequired) {
            this.consentRequired = consentRequired;
        }

        @Override
        public SocialProvider getProvider() {
            return SocialProvider.KAKAO;
        }

        @Override
        public KakaoFriends getFriendList(String accessToken, Integer offset, Integer limit) {
            if (consentRequired) {
                throw new MemberCustomException(MemberErrorCode.KAKAO_FRIEND_API_ERROR);
            }
            return KakaoFriends.of(
                Collections.emptyList(),
                0,
                null,
                null,
                0
            );
        }
    }

    // Note: GlobalJwtAdapter, GlobalCookieAdapter, and BlacklistAdapter are now concrete classes.
    // Tests should use @MockitoBean to mock them directly when needed.
}
