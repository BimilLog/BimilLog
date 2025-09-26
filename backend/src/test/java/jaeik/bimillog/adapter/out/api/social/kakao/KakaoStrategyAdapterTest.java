package jaeik.bimillog.adapter.out.api.social.kakao;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import jaeik.bimillog.infrastructure.adapter.out.api.social.kakao.KakaoApiClient;
import jaeik.bimillog.infrastructure.adapter.out.api.social.kakao.KakaoAuthClient;
import jaeik.bimillog.infrastructure.adapter.out.api.social.kakao.KakaoStrategyAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.KakaoTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static jaeik.bimillog.testutil.AuthTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>KakaoStrategyAdapter 단위 테스트</h2>
 * <p>카카오 소셜 로그인 전략의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("KakaoStrategyAdapter 단위 테스트")
@Tag("fast")
class KakaoStrategyAdapterTest extends BaseUnitTest {

    private static final String KAKAO_CLIENT_ID = "test-client-id";
    private static final String KAKAO_CLIENT_SECRET = "test-client-secret";
    private static final String KAKAO_REDIRECT_URI = "http://test.com/callback";
    private static final String KAKAO_ADMIN_KEY = "test-admin-key";

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private KakaoApiClient kakaoApiClient;

    private KakaoStrategyAdapter kakaoStrategyAdapter;

    @BeforeEach
    void setUp() {
        // KakaoKeyVO Mock 설정
        lenient().when(kakaoKeyVO.getCLIENT_ID()).thenReturn(KAKAO_CLIENT_ID);
        lenient().when(kakaoKeyVO.getCLIENT_SECRET()).thenReturn(KAKAO_CLIENT_SECRET);
        lenient().when(kakaoKeyVO.getREDIRECT_URI()).thenReturn(KAKAO_REDIRECT_URI);
        lenient().when(kakaoKeyVO.getADMIN_KEY()).thenReturn(KAKAO_ADMIN_KEY);

        kakaoStrategyAdapter = new KakaoStrategyAdapter(kakaoKeyVO, kakaoAuthClient, kakaoApiClient);
    }

    @Test
    @DisplayName("getSupportedProvider는 KAKAO를 반환한다")
    void shouldReturnKakaoProvider() {
        // When
        SocialProvider provider = kakaoStrategyAdapter.getSupportedProvider();

        // Then
        assertThat(provider).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("authenticate 성공 - 토큰 발급과 사용자 정보 조회")
    void shouldAuthenticateSuccessfully() {
        // Given - KakaoTestDataBuilder를 사용한 응답 데이터 생성
        Map<String, Object> tokenResponse = KakaoTestDataBuilder.createTokenResponse(
            TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        
        Map<String, Object> userInfoResponse = new java.util.HashMap<>();
        userInfoResponse.put("id", 123456789L);

        Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("nickname", TEST_SOCIAL_NICKNAME);
        profile.put("thumbnail_image_url", TEST_PROFILE_IMAGE);
        profile.put("profile_image_url", TEST_PROFILE_IMAGE);
        profile.put("is_default_image", false);

        Map<String, Object> kakaoAccount = new java.util.HashMap<>();
        kakaoAccount.put("email", TEST_EMAIL);
        kakaoAccount.put("profile", profile);
        kakaoAccount.put("has_email", TEST_EMAIL != null);
        kakaoAccount.put("email_needs_agreement", false);
        kakaoAccount.put("is_email_valid", true);
        kakaoAccount.put("is_email_verified", true);

        userInfoResponse.put("kakao_account", kakaoAccount);
        userInfoResponse.put("connected_at", "2024-01-01T00:00:00Z");

        given(kakaoAuthClient.getToken(anyString(), any(Map.class))).willReturn(tokenResponse);
        given(kakaoApiClient.getUserInfo(anyString())).willReturn(userInfoResponse);

        // When
        SocialUserProfile result = kakaoStrategyAdapter.authenticate(
            SocialProvider.KAKAO, TEST_AUTH_CODE);

        // Then
        assertThat(result).isNotNull();
        // KakaoTestDataBuilder가 숫자로 변환할 수 없는 TEST_SOCIAL_ID를 기본값으로 처리
        assertThat(result.socialId()).isEqualTo("123456789");
        assertThat(result.email()).isNull(); // 카카오는 이메일을 제공하지 않음
        assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(result.nickname()).isEqualTo(TEST_SOCIAL_NICKNAME);
        assertThat(result.profileImageUrl()).isEqualTo(TEST_PROFILE_IMAGE);
        assertThat(result.TemporaryToken()).satisfies(token -> {
            assertThat(token.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(token.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
        });

        verify(kakaoAuthClient).getToken(anyString(), argThat(params -> {
            Map<String, String> expectedParams = (Map<String, String>) params;
            return expectedParams.get("grant_type").equals("authorization_code") &&
                   expectedParams.get("client_id").equals(KAKAO_CLIENT_ID) &&
                   expectedParams.get("code").equals(TEST_AUTH_CODE);
        }));
        verify(kakaoApiClient).getUserInfo(eq("Bearer " + TEST_ACCESS_TOKEN));
    }

    @Test
    @DisplayName("authenticate 실패 - 토큰 발급 실패 시 예외 발생")
    void shouldThrowExceptionWhenTokenRequestFails() {
        // Given
        given(kakaoAuthClient.getToken(anyString(), any(Map.class)))
            .willThrow(new RuntimeException("Token request failed"));

        // When & Then
        assertThatThrownBy(() ->
            kakaoStrategyAdapter.authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Kakao TemporaryToken request failed");
    }

    @Test
    @DisplayName("authenticate 실패 - 사용자 정보 조회 실패 시 예외 발생")
    void shouldThrowExceptionWhenUserInfoRequestFails() {
        // Given
        Map<String, Object> tokenResponse = KakaoTestDataBuilder.createTokenResponse(
            TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);

        given(kakaoAuthClient.getToken(anyString(), any(Map.class))).willReturn(tokenResponse);
        given(kakaoApiClient.getUserInfo(anyString()))
            .willThrow(new RuntimeException("User info request failed"));

        // When & Then
        assertThatThrownBy(() ->
            kakaoStrategyAdapter.authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Kakao user info request failed");
    }

    @Test
    @DisplayName("unlink 성공 - 카카오 계정 연결 해제")
    void shouldUnlinkSuccessfully() {
        // Given
        // unlink는 void 메서드이므로 반환값이 없음
        doNothing().when(kakaoApiClient).unlink(anyString(), any(Map.class));

        // When
        kakaoStrategyAdapter.unlink(SocialProvider.KAKAO, TEST_SOCIAL_ID);

        // Then
        verify(kakaoApiClient).unlink(
            eq("KakaoAK " + KAKAO_ADMIN_KEY),
            argThat(params -> {
                Map<String, String> expectedParams = (Map<String, String>) params;
                return expectedParams.get("target_id_type").equals("user_id") &&
                       expectedParams.get("target_id").equals(TEST_SOCIAL_ID);
            })
        );
    }

    @Test
    @DisplayName("unlink 실패 시 예외 발생")
    void shouldThrowExceptionWhenUnlinkFails() {
        // Given
        doThrow(new RuntimeException("Unlink failed"))
            .when(kakaoApiClient).unlink(anyString(), any(Map.class));

        // When & Then
        assertThatThrownBy(() ->
            kakaoStrategyAdapter.unlink(SocialProvider.KAKAO, TEST_SOCIAL_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Kakao unlink failed");
    }

    @Test
    @DisplayName("logout 성공")
    void shouldLogoutSuccessfully() throws Exception {
        // Given
        doNothing().when(kakaoApiClient).logout(anyString(), anyString());

        // When
        kakaoStrategyAdapter.logout(SocialProvider.KAKAO, TEST_ACCESS_TOKEN);

        // Then
        verify(kakaoApiClient).logout(
            eq("Bearer " + TEST_ACCESS_TOKEN),
            eq("application/x-www-form-urlencoded;charset=utf-8")
        );
    }

    @Test
    @DisplayName("logout 실패시 예외를 상위로 전파한다")
    void shouldPropagateExceptionWhenLogoutFails() {
        // Given
        RuntimeException expectedException = new RuntimeException("Logout failed");
        doThrow(expectedException)
            .when(kakaoApiClient).logout(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() ->
            kakaoStrategyAdapter.logout(SocialProvider.KAKAO, TEST_ACCESS_TOKEN))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Logout failed");

        verify(kakaoApiClient).logout(anyString(), anyString());
    }
}
