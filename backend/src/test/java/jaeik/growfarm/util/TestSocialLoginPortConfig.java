package jaeik.growfarm.util;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.TokenVO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
    public SocialLoginPort testSocialLoginPort() {
        return new SocialLoginPort() {
            @Override
            public LoginResult login(SocialProvider provider, String code) {
                // 테스트용 더미 구현
                SocialUserProfile profile = new SocialUserProfile(
                    "test-social-id", 
                    "test@example.com", 
                    provider, 
                    "Test User", 
                    "https://example.com/profile.jpg"
                );
                TokenVO token = new TokenVO("dummy-access-token", "dummy-refresh-token");
                return new LoginResult(profile, token, false);
            }

            @Override
            public void unlink(SocialProvider provider, String socialId) {
                // 테스트 환경에서는 실제 API 호출하지 않음 - 성공으로 처리
                System.out.println("테스트 환경: 소셜 연결 해제 성공 (Mock)");
            }

            @Override
            public void logout(SocialProvider provider, String accessToken) {
                // 테스트 환경에서는 실제 API 호출하지 않음 - 성공으로 처리
                System.out.println("테스트 환경: 소셜 로그아웃 성공 (Mock)");
            }
        };
    }
}