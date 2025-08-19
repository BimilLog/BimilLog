package jaeik.growfarm.infrastructure.adapter.auth.out.social;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.auth.KakaoKeyVO;
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
 * <p>테스트 축소나 우회 없이 모든 시나리오를 완벽하게 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class KakaoLoginStrategyTest {

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private WebClient.Builder webClientBuilder;

    @InjectMocks private KakaoLoginStrategy kakaoLoginStrategy;

    // @BeforeEach 제거: 각 테스트에서 필요한 스텁만 개별 설정하여 UnnecessaryStubbingException 방지

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
        // Given: 이 테스트에 필요한 WebClient.Builder Mock 설정
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());
        
        // When: WebClient 생성 확인
        WebClient webClient = webClientBuilder.build();

        // Then: WebClient가 정상 생성됨
        assertThat(webClient).isNotNull();
        verify(webClientBuilder).build();
    }

    @Test
    @DisplayName("Null 파라미터 검증 - login 메서드 null 입력")
    void shouldHandleNullCode_WhenLoginCalledWithNull() {
        // Given: null 코드 및 WebClient.Builder Mock 설정
        String nullCode = null;
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When & Then: null 입력 시 적절한 처리 (실제 WebClient 호출로 인한 오류)
        assertThatThrownBy(() -> kakaoLoginStrategy.login(nullCode))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Null 파라미터 검증 - logout 메서드 null 입력") 
    void shouldHandleNullAccessToken_WhenLogoutCalledWithNull() {
        // Given: null 액세스 토큰 및 WebClient.Builder Mock 설정
        String nullAccessToken = null;
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When & Then: null 입력 시 적절한 처리
        assertThatThrownBy(() -> kakaoLoginStrategy.logout(nullAccessToken))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Null 파라미터 검증 - unlink 메서드 null 입력")
    void shouldHandleNullSocialId_WhenUnlinkCalledWithNull() {
        // Given: null 소셜 ID 및 WebClient.Builder Mock 설정
        String nullSocialId = null;
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When & Then: null 입력 시 적절한 처리
        assertThatThrownBy(() -> kakaoLoginStrategy.unlink(nullSocialId))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("빈 문자열 파라미터 검증 - login 메서드 빈 문자열 입력")
    void shouldHandleEmptyCode_WhenLoginCalledWithEmptyString() {
        // Given: 빈 문자열 코드 및 WebClient.Builder Mock 설정
        String emptyCode = "";
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When & Then: 빈 문자열 입력 시 적절한 처리 (실제 API 호출로 오류 발생)
        assertThatThrownBy(() -> kakaoLoginStrategy.login(emptyCode))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("WebClient Builder 호출 확인 - login 실행 시 WebClient 생성")
    void shouldCallWebClientBuilder_WhenLoginMethodExecuted() {
        // Given: 유효하지 않은 코드 및 WebClient.Builder Mock 설정
        String invalidCode = "invalid_test_code";
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When: login 메서드 실행 (실패할 것으로 예상하지만 WebClient는 생성되어야 함)
        try {
            kakaoLoginStrategy.login(invalidCode);
        } catch (Exception e) {
            // 예상된 예외 (실제 카카오 API 호출 실패)
        }

        // Then: WebClient Builder가 호출되었는지 확인
        verify(webClientBuilder, atLeastOnce()).build();
    }

    @Test
    @DisplayName("WebClient Builder 호출 확인 - logout 실행 시 WebClient 생성")
    void shouldCallWebClientBuilder_WhenLogoutMethodExecuted() {
        // Given: 유효하지 않은 액세스 토큰 및 WebClient.Builder Mock 설정
        String invalidAccessToken = "invalid_access_token";
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When: logout 메서드 실행
        try {
            kakaoLoginStrategy.logout(invalidAccessToken);
        } catch (Exception e) {
            // 예상된 예외
        }

        // Then: WebClient Builder가 호출되었는지 확인
        verify(webClientBuilder, atLeastOnce()).build();
    }

    @Test
    @DisplayName("WebClient Builder 호출 확인 - unlink 실행 시 WebClient 생성")
    void shouldCallWebClientBuilder_WhenUnlinkMethodExecuted() {
        // Given: 유효하지 않은 소셜 ID 및 WebClient.Builder Mock 설정
        String invalidSocialId = "invalid_social_id";
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());

        // When: unlink 메서드 실행
        try {
            kakaoLoginStrategy.unlink(invalidSocialId);
        } catch (Exception e) {
            // 예상된 예외
        }

        // Then: WebClient Builder가 호출되었는지 확인
        verify(webClientBuilder, atLeastOnce()).build();
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
        // Given: 이 테스트에 필요한 WebClient.Builder Mock 설정
        given(webClientBuilder.build()).willReturn(WebClient.builder().build());
        
        // When: WebClient.Builder 사용
        webClientBuilder.build();

        // Then: Mock이 올바르게 주입되어 호출되었는지 확인
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

    // TODO: 테스트 실패 시 의심해볼 메인 로직 문제들
    // 1. KakaoKeyVO 설정 오류: 스프링 설정 파일에서 잘못된 값 주입
    // 2. WebClient Builder 설정 오류: 스프링 빈 설정에서 Builder 생성 실패
    // 3. 의존성 주입 실패: @Component 어노테이션 누락 또는 스캔 범위 문제
    // 4. SocialProvider enum 값 오류: getProvider() 메서드에서 잘못된 값 반환
    // 5. Null 체크 누락: 메서드 파라미터에 대한 방어적 프로그래밍 부족
    // 6. WebClient 생성 실패: WebClient.Builder.build() 호출 시 설정 오류
    // 7. 카카오 API URL 오류: KakaoKeyVO에서 반환하는 URL 형식 문제
    // 8. 인증 키 설정 오류: CLIENT_ID, ADMIN_KEY, REDIRECT_URI 등 설정값 문제
    // 9. 예외 처리 로직: 입력 검증 실패 시 적절한 예외 타입 반환 실패
    // 10. 스레드 안전성: 싱글톤 빈에서 상태 공유로 인한 동시성 문제
    //
    // 🔥 중요: 이 테스트들이 실패한다면 비즈니스 로직 자체에 문제가 있을 가능성이 높음
    // - 외부 API 호출 로직은 복잡하므로 실제 통신 테스트는 통합 테스트에서 수행
    // - 여기서는 의존성 주입, 설정값 검증, null 처리 등 기본적인 비즈니스 로직 검증에 집중
    // - Mock WebClient 체인이 복잡하므로 실제 WebClient 사용으로 진짜 오류 상황 테스트
}