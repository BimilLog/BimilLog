package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.TokenVO;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.SocialLoginStrategy;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>SocialLoginAdapter 단위 테스트</h2>
 * <p>소셜 로그인 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>Strategy 패턴과 기존/신규 사용자 처리 로직 완벽 검증</p>
 *
 * @author Jaeik
 * @version  2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SocialLoginAdapterTest {

    @Mock private SocialLoginStrategy kakaoStrategy;
    @Mock private UserQueryUseCase userQueryUseCase;

    private SocialLoginAdapter socialLoginAdapter;

    private SocialLoginUserData testUserData;
    private SocialLoginPort.SocialUserProfile testUserProfile;
    private TokenVO testTokenVO;
    private SocialLoginStrategy.StrategyLoginResult testStrategyResult;

    @BeforeEach
    void setUp() {
        // 🔥 CRITICAL: Mock 설정을 생성자 호출 전에 수행
        // NPE 방지: SocialLoginAdapter 생성자에서 fulltext.getProvider() 호출 시 null 방지
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);
        
        // SocialLoginAdapter 생성자에 전략 리스트 전달
        socialLoginAdapter = new SocialLoginAdapter(List.of(kakaoStrategy), userQueryUseCase);

        // 테스트 데이터 설정
        testUserData = new SocialLoginUserData("123456789", "test@example.com", 
                SocialProvider.KAKAO, "테스트사용자", "http://profile.image.url", "fcm-token");
        testUserProfile = new SocialLoginPort.SocialUserProfile("123456789", "test@example.com", 
                SocialProvider.KAKAO, "테스트사용자", "http://profile.image.url");
        
        testTokenVO = TokenVO.builder()
                .accessToken("access-token-12345")
                .refreshToken("refresh-token-12345")
                .build();

        testStrategyResult = new SocialLoginStrategy.StrategyLoginResult(testUserData, testTokenVO);
    }

    @Test
    @DisplayName("소셜 로그인 - 신규 사용자 로그인")
    void shouldReturnNewUserLogin_WhenUserNotExists() {
        // Given: 소셜 전략과 사용자 조회 서비스 설정
        String code = "auth-code";
        given(kakaoStrategy.login(code)).willReturn(Mono.just(testStrategyResult));
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.empty());

        // When: 소셜 로그인 실행
        SocialLoginPort.LoginResult result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 신규 사용자 로그인 결과 반환
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.userProfile()).isEqualTo(testUserProfile);
        assertThat(result.token()).isEqualTo(testTokenVO);
        
        verify(kakaoStrategy).login(code);
        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
    }

    @Test
    @DisplayName("소셜 로그인 - 기존 사용자 로그인")
    void shouldReturnExistingUserLogin_WhenUserExists() {
        // Given: 기존 사용자가 존재하는 경우
        String code = "auth-code";
        Setting setting = Setting.builder().build();
        User existingUser = User.builder()
                .id(1L)
                .userName("기존사용자")
                .socialId("123456789")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        
        given(kakaoStrategy.login(code)).willReturn(Mono.just(testStrategyResult));
        given(userQueryUseCase.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(Optional.of(existingUser));

        // When: 소셜 로그인 실행
        SocialLoginPort.LoginResult result = socialLoginAdapter.login(SocialProvider.KAKAO, code);

        // Then: 기존 사용자 로그인 결과 반환
        assertThat(result.isNewUser()).isFalse();
        assertThat(result.userProfile()).isEqualTo(testUserProfile);
        assertThat(result.token()).isEqualTo(testTokenVO);
        
        verify(kakaoStrategy).login(code);
        verify(userQueryUseCase).findByProviderAndSocialId(SocialProvider.KAKAO, "123456789");
    }
}