package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.out.KakaoFriendPort;
import jaeik.bimillog.domain.user.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.domain.user.entity.Token;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

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
                Token token = Token.createTemporaryToken("dummy-access-token", "dummy-refresh-token");
                
                return new LoginResult(profile, token, isNewUser);
            }

            @Override
            public void logout(SocialProvider provider, String accessToken) {
                // 테스트용 더미 구현 - 아무 작업도 하지 않음
            }

            @Override
            public void unlink(SocialProvider provider, String socialId) {
                // 테스트용 더미 구현 - 아무 작업도 하지 않음
            }
        };
    }

    @Bean
    @Primary
    public KakaoFriendPort testKakaoFriendPort() {
        return new KakaoFriendPort() {
            @Override
            public Mono<KakaoFriendsResponseVO> getFriendList(String accessToken, Integer offset, Integer limit) {
                // 테스트용 더미 친구 목록 응답
                KakaoFriendsResponseVO response = KakaoFriendsResponseVO.of(
                    Collections.emptyList(), // 빈 친구 목록
                    0, // 전체 친구 수
                    null, // 이전 페이지 URL
                    null, // 다음 페이지 URL  
                    0 // 즐겨찾기 친구 수
                );
                return Mono.just(response);
            }
        };
    }
}