package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.application.strategy.SocialFriendStrategy;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.out.global.GlobalCookieAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;

/**
 * <h2>테스트용 소셜 로그인 설정</h2>
 * <p>통합 테스트에서 외부 소셜 연동을 대체하는 전략을 제공합니다.</p>
 */
@TestConfiguration
public class TestSocialLoginPortConfig {

    private static final TestSocialPlatformStrategy TEST_PLATFORM_STRATEGY = new TestSocialPlatformStrategy();

    @Bean
    @Primary
    public GlobalSocialStrategyPort testGlobalSocialStrategyPort() {
        return provider -> {
            if (provider == SocialProvider.KAKAO) {
                return TEST_PLATFORM_STRATEGY;
            }
            throw new IllegalArgumentException("지원하지 않는 테스트 소셜 제공자: " + provider);
        };
    }

    private static final class TestSocialPlatformStrategy extends SocialPlatformStrategy {

        TestSocialPlatformStrategy() {
            super(SocialProvider.KAKAO, new TestSocialAuthStrategy(), new TestSocialFriendStrategy());
        }
    }

    private static final class TestSocialAuthStrategy implements SocialAuthStrategy {

        @Override
        public SocialProvider getProvider() {
            return SocialProvider.KAKAO;
        }

        @Override
        public SocialMemberProfile getSocialToken(String code) {
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
                refreshToken,
                null
            );
        }

        @Override
        public void getUserInfo(String accessToken) {
            // no-op
        }

        @Override
        public void unlink(String socialId) {
            // no-op
        }

        @Override
        public void logout(String accessToken) {
            // no-op
        }
    }

    private static final class TestSocialFriendStrategy implements SocialFriendStrategy {

        @Override
        public SocialProvider getProvider() {
            return SocialProvider.KAKAO;
        }

        @Override
        public KakaoFriendsResponseVO getFriendList(String accessToken, Integer offset, Integer limit) {
            return KakaoFriendsResponseVO.of(
                Collections.emptyList(),
                0,
                null,
                null,
                0
            );
        }
    }

    @Bean
    @Primary
    public BlacklistPort testBlacklistPort() {
        return new BlacklistPort() {
            @Override
            public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
                return false;
            }

            @Override
            public void saveBlackList(jaeik.bimillog.domain.auth.entity.BlackList blackList) {
                // no-op
            }
        };
    }

    @Bean
    @Primary
    public GlobalJwtPort testGlobalJwtPort() {
        return new GlobalJwtPort() {
            @Override
            public String generateAccessToken(MemberDetail userDetail) {
                return "test-access-TemporaryToken";
            }

            @Override
            public String generateRefreshToken(MemberDetail userDetail) {
                return "test-refresh-TemporaryToken";
            }

            @Override
            public boolean validateToken(String token) {
                return true;
            }

            @Override
            public MemberDetail getUserInfoFromToken(String jwtAccessToken) {
                return MemberDetail.ofExisting(TestMembers.MEMBER_1, 1L, 1L);
            }

            @Override
            public Long getTokenIdFromToken(String jwtRefreshToken) {
                return 1L;
            }

            @Override
            public boolean shouldRefreshToken(String token, long thresholdDays) {
                return false;
            }

            @Override
            public String generateTokenHash(String token) {
                return "test-TemporaryToken-hash";
            }
        };
    }

    @Bean
    @Primary
    public GlobalCookiePort testGlobalCookiePort() {
        return new GlobalCookieAdapter();
    }
}
