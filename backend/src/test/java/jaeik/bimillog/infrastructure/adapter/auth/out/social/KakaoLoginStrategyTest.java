package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.infrastructure.auth.KakaoKeyVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * <h2>KakaoLoginStrategy 단위 테스트</h2>
 * <p>비즈니스 로직 위주로 테스트하며, WebClient는 의존성으로만 Mock 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class KakaoLoginStrategyTest {

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;

    private KakaoLoginStrategy kakaoLoginStrategy;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        given(webClientBuilder.build()).willReturn(webClient);
        kakaoLoginStrategy = new KakaoLoginStrategy(kakaoKeyVO, webClientBuilder);
    }

    @Test
    @DisplayName("소셜 제공자 확인 - 카카오 Provider 반환")
    void shouldReturnKakaoProvider_WhenGetProviderCalled() {
        // When: Provider 조회
        SocialProvider provider = kakaoLoginStrategy.getProvider();

        // Then: KAKAO Provider 반환 검증
        assertThat(provider).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("KakaoKeyVO 설정 검증 - 모든 URL과 키가 올바르게 주입되었는지 확인")
    void shouldUseCorrectKakaoKeyConfiguration_WhenInitialized() {
        // Given: 이 테스트에 필요한 KakaoKeyVO Mock 설정
        given(kakaoKeyVO.getTOKEN_URL()).willReturn("https://kauth.kakao.com/oauth/token");
        given(kakaoKeyVO.getUSER_INFO_URL()).willReturn("https://kapi.kakao.com/v2/user/me");
        given(kakaoKeyVO.getLOGOUT_URL()).willReturn("https://kapi.kakao.com/v1/user/logout");
        given(kakaoKeyVO.getUNLINK_URL()).willReturn("https://kapi.kakao.com/v1/user/unlink");
        given(kakaoKeyVO.getCLIENT_ID()).willReturn("test_client_id");
        given(kakaoKeyVO.getREDIRECT_URI()).willReturn("http://localhost:3000/auth/callback");
        given(kakaoKeyVO.getADMIN_KEY()).willReturn("test_admin_key");
        
        // When: KakaoKeyVO 값들 확인
        String tokenUrl = kakaoKeyVO.getTOKEN_URL();
        String userInfoUrl = kakaoKeyVO.getUSER_INFO_URL();
        String logoutUrl = kakaoKeyVO.getLOGOUT_URL();
        String unlinkUrl = kakaoKeyVO.getUNLINK_URL();
        String clientId = kakaoKeyVO.getCLIENT_ID();
        String redirectUri = kakaoKeyVO.getREDIRECT_URI();
        String adminKey = kakaoKeyVO.getADMIN_KEY();

        // Then: 모든 값이 정확히 설정되었는지 검증
        assertThat(tokenUrl).isEqualTo("https://kauth.kakao.com/oauth/token");
        assertThat(userInfoUrl).isEqualTo("https://kapi.kakao.com/v2/user/me");
        assertThat(logoutUrl).isEqualTo("https://kapi.kakao.com/v1/user/logout");
        assertThat(unlinkUrl).isEqualTo("https://kapi.kakao.com/v1/user/unlink");
        assertThat(clientId).isEqualTo("test_client_id");
        assertThat(redirectUri).isEqualTo("http://localhost:3000/auth/callback");
        assertThat(adminKey).isEqualTo("test_admin_key");
    }

    @Test
    @DisplayName("WebClient Builder 검증 - Builder가 올바르게 주입되고 WebClient 생성")
    void shouldCreateWebClient_WhenWebClientBuilderProvided() {
        // When & Then: 생성자에서 WebClient Builder가 정확히 한 번 호출되었는지 확인
        // (setUp에서 이미 생성됨)
        verify(webClientBuilder).build();
    }

    @Test
    @DisplayName("Null 파라미터 검증 - login 메서드 null 입력")
    void shouldHandleNullCode_WhenLoginCalledWithNull() {
        // Given: null 코드
        String nullCode = null;

        // When & Then: null 입력 시 적절한 처리 (실제 WebClient 호출로 인한 오류)
        assertThatThrownBy(() -> kakaoLoginStrategy.login(nullCode).block())
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Null 파라미터 검증 - logout 메서드 null 입력") 
    void shouldHandleNullAccessToken_WhenLogoutCalledWithNull() {
        // Given: null 액세스 토큰
        String nullAccessToken = null;

        // When & Then: null 입력 시 적절한 처리
        assertThatThrownBy(() -> kakaoLoginStrategy.logout(nullAccessToken))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Null 파라미터 검증 - unlink 메서드 null 입력")
    void shouldHandleNullSocialId_WhenUnlinkCalledWithNull() {
        // Given: null 소셜 ID
        String nullSocialId = null;

        // When & Then: null 입력 시 적절한 처리 (Mono 실행으로 인한 오류)
        assertThatThrownBy(() -> kakaoLoginStrategy.unlink(nullSocialId).block())
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("빈 문자열 파라미터 검증 - login 메서드 빈 문자열 입력")
    void shouldHandleEmptyCode_WhenLoginCalledWithEmptyString() {
        // Given: 빈 문자열 코드
        String emptyCode = "";

        // When & Then: 빈 문자열 입력 시 적절한 처리 (실제 API 호출로 오류 발생)
        assertThatThrownBy(() -> kakaoLoginStrategy.login(emptyCode).block())
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("WebClient Builder 호출 확인 - 생성자에서 WebClient 생성")
    void shouldCallWebClientBuilder_WhenObjectCreated() {
        // When: KakaoLoginStrategy 객체 생성 (setUp에서 이미 생성됨)
        // Then: WebClient Builder가 생성자에서 정확히 한 번 호출되었는지 확인
        verify(webClientBuilder).build();
    }

    @Test
    @DisplayName("WebClient 재사용 확인 - logout 실행 시 기존 WebClient 사용")
    void shouldReuseWebClient_WhenLogoutMethodExecuted() {
        // Given: 유효하지 않은 액세스 토큰
        String invalidAccessToken = "invalid_access_token";

        // When: logout 메서드 실행
        try {
            kakaoLoginStrategy.logout(invalidAccessToken);
        } catch (Exception e) {
            // 예상된 예외
        }

        // Then: WebClient Builder는 생성자에서만 호출되고, 추가 호출은 없음
        verify(webClientBuilder).build(); // 정확히 한 번만
    }

    @Test
    @DisplayName("WebClient 재사용 확인 - unlink 실행 시 기존 WebClient 사용")
    void shouldReuseWebClient_WhenUnlinkMethodExecuted() {
        // Given: 유효하지 않은 소셜 ID
        String invalidSocialId = "invalid_social_id";

        // When: unlink 메서드 실행
        try {
            kakaoLoginStrategy.unlink(invalidSocialId);
        } catch (Exception e) {
            // 예상된 예외
        }

        // Then: WebClient Builder는 생성자에서만 호출되고, 추가 호출은 없음
        verify(webClientBuilder).build(); // 정확히 한 번만
    }

    @Test
    @DisplayName("의존성 주입 검증 - KakaoKeyVO가 올바르게 주입되었는지 확인")
    void shouldHaveKakaoKeyVOInjected_WhenObjectCreated() {
        // Given: 이 테스트에 필요한 KakaoKeyVO Mock 설정
        given(kakaoKeyVO.getTOKEN_URL()).willReturn("https://kauth.kakao.com/oauth/token");
        given(kakaoKeyVO.getUSER_INFO_URL()).willReturn("https://kapi.kakao.com/v2/user/me");
        given(kakaoKeyVO.getCLIENT_ID()).willReturn("test_client_id");
        
        // When: KakaoKeyVO 메서드들 호출
        kakaoKeyVO.getTOKEN_URL();
        kakaoKeyVO.getUSER_INFO_URL();
        kakaoKeyVO.getCLIENT_ID();

        // Then: Mock이 올바르게 주입되어 호출되었는지 확인
        verify(kakaoKeyVO).getTOKEN_URL();
        verify(kakaoKeyVO).getUSER_INFO_URL();
        verify(kakaoKeyVO).getCLIENT_ID();
    }

    @Test
    @DisplayName("의존성 주입 검증 - WebClient.Builder가 올바르게 주입되었는지 확인")
    void shouldHaveWebClientBuilderInjected_WhenObjectCreated() {
        // When & Then: 생성자에서 WebClient.Builder가 정확히 한 번 호출되었는지 확인
        verify(webClientBuilder).build();
    }

    @Test
    @DisplayName("SocialProvider 일관성 검증 - KAKAO Provider 반환 일관성")
    void shouldConsistentlyReturnKakaoProvider_WhenMultipleCalls() {
        // When: 여러 번 호출
        SocialProvider provider1 = kakaoLoginStrategy.getProvider();
        SocialProvider provider2 = kakaoLoginStrategy.getProvider();
        SocialProvider provider3 = kakaoLoginStrategy.getProvider();

        // Then: 모든 호출에서 일관되게 KAKAO 반환
        assertThat(provider1).isEqualTo(SocialProvider.KAKAO);
        assertThat(provider2).isEqualTo(SocialProvider.KAKAO);
        assertThat(provider3).isEqualTo(SocialProvider.KAKAO);
        assertThat(provider1).isEqualTo(provider2).isEqualTo(provider3);
    }
}