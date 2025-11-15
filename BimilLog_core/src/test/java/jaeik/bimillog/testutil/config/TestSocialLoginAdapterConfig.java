package jaeik.bimillog.testutil.config;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.out.GlobalSocialStrategyAdapter;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.dto.KakaoFriendsDTO;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoApiClient;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoFriendClient;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.builder.KakaoTestDataBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.Map;

/**
 * <h2>테스트용 소셜 로그인 설정</h2>
 * <p>통합 테스트에서 외부 소셜 연동을 대체하는 전략을 제공합니다.</p>
 */
@TestConfiguration
public class TestSocialLoginAdapterConfig {

    private static final TestSocialPlatformStrategy TEST_PLATFORM_STRATEGY =
            new TestSocialPlatformStrategy();
    private static final TestKakaoApiClient TEST_KAKAO_API_CLIENT = new TestKakaoApiClient();

    @Bean
    @Primary
    public GlobalSocialStrategyAdapter testGlobalSocialStrategyAdapter() {
        return new GlobalSocialStrategyAdapter(Collections.singletonList(TEST_PLATFORM_STRATEGY));
    }

    @Bean
    @Primary
    public KakaoFriendClient testKakaoFriendClient() {
        return new KakaoFriendClient(TEST_KAKAO_API_CLIENT);
    }

    public static void setFriendConsentRequired(boolean required) {
        TEST_KAKAO_API_CLIENT.setConsentRequired(required);
    }

    private static final class TestSocialPlatformStrategy extends SocialPlatformStrategy {

        TestSocialPlatformStrategy() {
            super(SocialProvider.KAKAO, new TestSocialAuthStrategy());
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

    private static final class TestKakaoApiClient implements KakaoApiClient {

        private boolean consentRequired;

        void setConsentRequired(boolean consentRequired) {
            this.consentRequired = consentRequired;
        }

        @Override
        public Map<String, Object> getUserInfo(String authorization) {
            return Collections.emptyMap();
        }

        @Override
        public void logout(String authorization) {
            // no-op
        }

        @Override
        public void forceLogout(String authorization, Map<String, String> params) {
            // no-op
        }

        @Override
        public void unlink(String authorization, Map<String, String> params) {
            // no-op
        }

        @Override
        public KakaoFriendsDTO getFriends(String authorization, Integer offset, Integer limit) {
            if (consentRequired) {
                throw new CustomException(ErrorCode.MEMBER_KAKAO_FRIEND_API_ERROR);
            }
            return KakaoTestDataBuilder.createKakaoFriendsResponse(
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
