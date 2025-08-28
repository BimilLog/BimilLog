package jaeik.growfarm.util;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
import jaeik.growfarm.domain.auth.entity.TempUserData;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.TokenVO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

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
                String socialId;
                boolean isNewUser;
                
                // code 값에 따라 신규 사용자/기존 사용자 구분
                if ("new_user_code".equals(code)) {
                    socialId = "new-user-social-id";
                    isNewUser = true;
                } else if ("existing_user_code".equals(code)) {
                    socialId = "test-social-id-12345"; // 통합 테스트에서 생성한 기존 사용자 ID와 일치
                    isNewUser = false;
                } else {
                    // 기본값은 기존 사용자로 처리
                    socialId = "test-social-id";
                    isNewUser = false;
                }
                
                SocialUserProfile profile = new SocialUserProfile(
                    socialId, 
                    "test@example.com", 
                    provider, 
                    "Test User", 
                    "https://example.com/profile.jpg"
                );
                TokenVO token = new TokenVO("dummy-access-token", "dummy-refresh-token");
                
                return new LoginResult(profile, token, isNewUser);
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

    @Bean
    @Primary
    public TempDataPort testTempDataPort() {
        return new TempDataPort() {
            @Override
            public void saveTempData(String uuid, SocialLoginPort.SocialUserProfile userProfile, TokenVO tokenVO, String fcmToken) {
                // 테스트 환경에서는 실제 Redis 저장하지 않음 - 성공으로 처리
                System.out.println("테스트 환경: 임시 데이터 저장 성공 (Mock) - UUID: " + uuid);
            }

            @Override
            public Optional<TempUserData> getTempData(String uuid) {
                // 특정 UUID에 대해서는 테스트용 임시 데이터 반환
                if ("integration-test-uuid-12345".equals(uuid)) {
                    SocialLoginPort.SocialUserProfile userProfile = new SocialLoginPort.SocialUserProfile(
                            "test-social-id",
                            "test@example.com", 
                            SocialProvider.KAKAO,
                            "Test User",
                            "https://example.com/profile.jpg"
                    );
                    
                    TokenVO tokenVO = new TokenVO("dummy-access-token", "dummy-refresh-token");
                    
                    TempUserData tempData = TempUserData.of(userProfile, tokenVO, "integration-test-fcm-token");
                    
                    return Optional.of(tempData);
                }
                return Optional.empty();
            }

            @Override
            public void removeTempData(String uuid) {
                // 테스트 환경에서는 실제 삭제하지 않음 - 성공으로 처리
                System.out.println("테스트 환경: 임시 데이터 삭제 성공 (Mock) - UUID: " + uuid);
            }

            @Override
            public ResponseCookie createTempCookie(String uuid) {
                return ResponseCookie.from("temp", uuid)
                        .maxAge(Duration.ofMinutes(30))
                        .httpOnly(true)
                        .build();
            }
        };
    }
}