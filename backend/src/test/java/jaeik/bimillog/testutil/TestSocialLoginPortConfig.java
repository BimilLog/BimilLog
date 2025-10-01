package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.KakaoMemberInfo;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.member.application.port.out.KakaoFriendPort;
import jaeik.bimillog.domain.member.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.entity.memberdetail.ExistingMemberDetail;
import jaeik.bimillog.infrastructure.adapter.out.global.GlobalCookieAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Collections;

/**
 * <h2>테스트용 소셜 로그인 포트 설정</h2>
 * <p>통합 테스트에서 실제 소셜 API 호출을 방지하기 위한 Mock 구현</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@TestConfiguration
public class TestSocialLoginPortConfig {

    @Bean
    @Primary
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SocialStrategyPort testSocialStrategyPort() {
        return new TestSocialStrategyPort();
    }

    private static final class TestSocialStrategyPort implements SocialStrategyPort, Ordered {

        @Override
        public SocialProvider getSupportedProvider() {
            return SocialProvider.KAKAO; // 테스트용으로 KAKAO 제공자 지원
        }

        @Override
        public KakaoToken getToken(String code) {
            return KakaoToken.of("dummy-access-token", "dummy-refresh-token");
        }

        @Override
        public KakaoMemberInfo getUserInfo(String accessToken) {
            String socialId;

            // accessToken에 따라 다른 회원 ID 반환 (테스트 목적)
            if ("new-member-token".equals(accessToken)) {
                socialId = "new-member-social-id";
            } else if ("existing-member-token".equals(accessToken)) {
                socialId = "test-social-id-12345"; // 통합 테스트에서 생성한 기존 회원 ID와 일치
            } else {
                socialId = "test-social-id";
            }

            return KakaoMemberInfo.of(
                socialId,
                null, // 카카오는 이메일을 제공하지 않음
                SocialProvider.KAKAO,
                "Test Member",
                "https://example.com/profile.jpg"
            );
        }

        @Override
        public void logout(SocialProvider provider, String accessToken) {
            // 테스트용 더미 구현 - 아무 작업도 하지 않음
        }

        @Override
        public void unlink(SocialProvider provider, String socialId) {
            // 테스트용 더미 구현 - 아무 작업도 하지 않음
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    @Bean
    @Primary
    public BlacklistPort testBlacklistPort() {
        return new BlacklistPort() {
            @Override
            public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
                // 테스트용 더미 구현 - 항상 false 반환 (블랙리스트에 없음)
                return false;
            }

            @Override
            public void saveBlackList(jaeik.bimillog.domain.auth.entity.BlackList blackList) {
                // 테스트용 더미 구현 - 아무 작업도 하지 않음
            }
        };
    }

    @Bean
    @Primary
    public KakaoFriendPort testKakaoFriendPort() {
        return new KakaoFriendPort() {
            @Override
            public KakaoFriendsResponseVO getFriendList(String accessToken, Integer offset, Integer limit) {
                // 테스트용 더미 친구 목록 응답
                KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(
                    Collections.emptyList(), // 빈 친구 목록
                    0, // 전체 친구 수
                    null, // 이전 페이지 URL
                    null, // 다음 페이지 URL
                    0 // 즐겨찾기 친구 수
                );
                return response;
            }
        };
    }

    @Bean
    @Primary
    public GlobalJwtPort testGlobalJwtPort() {
        return new GlobalJwtPort() {
            @Override
            public String generateAccessToken(ExistingMemberDetail userDetail) {
                return "test-access-TemporaryToken";
            }

            @Override
            public String generateRefreshToken(ExistingMemberDetail userDetail) {
                return "test-refresh-TemporaryToken";
            }

            @Override
            public boolean validateToken(String token) {
                return true;
            }

            @Override
            public ExistingMemberDetail getUserInfoFromToken(String jwtAccessToken) {
                return ExistingMemberDetail.of(TestMembers.MEMBER_1, 1L, 1L);
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
