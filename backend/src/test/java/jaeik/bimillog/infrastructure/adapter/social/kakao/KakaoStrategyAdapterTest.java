package jaeik.bimillog.infrastructure.adapter.social.kakao;

import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

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
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoStrategyAdapter 단위 테스트")
class KakaoStrategyAdapterTest {

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_REDIRECT_URI = "http://test.com/callback";
    private static final String TEST_ADMIN_KEY = "test-admin-key";
    private static final String TEST_AUTH_CODE = "test-auth-code";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private static final String TEST_SOCIAL_ID = "12345678";
    private static final String TEST_NICKNAME = "테스트유저";
    private static final String TEST_EMAIL = "test@kakao.com";
    private static final String TEST_PROFILE_IMAGE = "http://image.kakao.com/profile.jpg";

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private KakaoApiClient kakaoApiClient;

    private KakaoStrategyAdapter kakaoStrategyAdapter;

    @BeforeEach
    void setUp() {
        // Lenient stubbing으로 모든 테스트에서 사용되지 않아도 오류 발생하지 않음
        lenient().when(kakaoKeyVO.getCLIENT_ID()).thenReturn(TEST_CLIENT_ID);
        lenient().when(kakaoKeyVO.getCLIENT_SECRET()).thenReturn(TEST_CLIENT_SECRET);
        lenient().when(kakaoKeyVO.getREDIRECT_URI()).thenReturn(TEST_REDIRECT_URI);
        lenient().when(kakaoKeyVO.getADMIN_KEY()).thenReturn(TEST_ADMIN_KEY);

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
        // Given
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", TEST_ACCESS_TOKEN);
        tokenResponse.put("refresh_token", TEST_REFRESH_TOKEN);

        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("id", Long.parseLong(TEST_SOCIAL_ID));

        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", TEST_NICKNAME);
        profile.put("thumbnail_image_url", TEST_PROFILE_IMAGE);

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", TEST_EMAIL);
        kakaoAccount.put("profile", profile);
        userInfoResponse.put("kakao_account", kakaoAccount);

        given(kakaoAuthClient.getToken(anyString(), any(Map.class))).willReturn(tokenResponse);
        given(kakaoApiClient.getUserInfo(anyString())).willReturn(userInfoResponse);

        // When
        SocialAuthData.AuthenticationResult result = kakaoStrategyAdapter.authenticate(
            SocialProvider.KAKAO, TEST_AUTH_CODE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userProfile()).satisfies(userProfile -> {
            assertThat(userProfile.socialId()).isEqualTo(TEST_SOCIAL_ID);
            assertThat(userProfile.email()).isNull(); // 카카오는 이메일을 제공하지 않음
            assertThat(userProfile.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(userProfile.nickname()).isEqualTo(TEST_NICKNAME);
            assertThat(userProfile.profileImageUrl()).isEqualTo(TEST_PROFILE_IMAGE);
        });
        assertThat(result.token()).satisfies(token -> {
            assertThat(token.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(token.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
        });

        verify(kakaoAuthClient).getToken(anyString(), argThat(params -> {
            Map<String, String> expectedParams = (Map<String, String>) params;
            return expectedParams.get("grant_type").equals("authorization_code") &&
                   expectedParams.get("client_id").equals(TEST_CLIENT_ID) &&
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
            .hasMessageContaining("Kakao token request failed");
    }

    @Test
    @DisplayName("authenticate 실패 - 사용자 정보 조회 실패 시 예외 발생")
    void shouldThrowExceptionWhenUserInfoRequestFails() {
        // Given
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", TEST_ACCESS_TOKEN);
        tokenResponse.put("refresh_token", TEST_REFRESH_TOKEN);

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
            eq("KakaoAK " + TEST_ADMIN_KEY),
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
    @DisplayName("logout 성공 - 예외가 발생해도 무시한다")
    void shouldLogoutSuccessfully() {
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
    @DisplayName("logout 실패해도 예외를 던지지 않는다")
    void shouldNotThrowExceptionWhenLogoutFails() {
        // Given
        doThrow(new RuntimeException("Logout failed"))
            .when(kakaoApiClient).logout(anyString(), anyString());

        // When & Then - 예외가 발생하지 않아야 함
        kakaoStrategyAdapter.logout(SocialProvider.KAKAO, TEST_ACCESS_TOKEN);

        verify(kakaoApiClient).logout(anyString(), anyString());
    }
}