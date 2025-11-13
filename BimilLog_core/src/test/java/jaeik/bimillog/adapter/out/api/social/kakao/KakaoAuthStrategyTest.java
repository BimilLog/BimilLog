package jaeik.bimillog.adapter.out.api.social.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoApiClient;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoAuthClient;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoAuthStrategy;
import jaeik.bimillog.infrastructure.api.social.kakao.KakaoKeyVO;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>KakaoAuthStrategy 단위 테스트</h2>
 * <p>카카오 인증 전략의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 */
@DisplayName("KakaoAuthStrategy 단위 테스트")
@Tag("unit")
class KakaoAuthStrategyTest extends BaseUnitTest {

    private static final String KAKAO_CLIENT_ID = "test-client-id";
    private static final String KAKAO_CLIENT_SECRET = "test-client-secret";
    private static final String KAKAO_REDIRECT_URI = "http://test.com/callback";
    private static final String KAKAO_ADMIN_KEY = "test-admin-key";

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private KakaoApiClient kakaoApiClient;

    private KakaoAuthStrategy kakaoAuthStrategy;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // KakaoKeyVO Mock 설정
        lenient().when(kakaoKeyVO.getCLIENT_ID()).thenReturn(KAKAO_CLIENT_ID);
        lenient().when(kakaoKeyVO.getCLIENT_SECRET()).thenReturn(KAKAO_CLIENT_SECRET);
        lenient().when(kakaoKeyVO.getREDIRECT_URI()).thenReturn(KAKAO_REDIRECT_URI);
        lenient().when(kakaoKeyVO.getADMIN_KEY()).thenReturn(KAKAO_ADMIN_KEY);

        objectMapper = new ObjectMapper();
        kakaoAuthStrategy = new KakaoAuthStrategy(kakaoKeyVO, kakaoAuthClient, kakaoApiClient, objectMapper);
    }

    @Test
    @DisplayName("getProvider는 KAKAO를 반환한다")
    void shouldReturnKakaoProvider() {
        SocialProvider provider = kakaoAuthStrategy.getProvider();
        assertThat(provider).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("getSocialToken 성공 - 토큰 발급 및 id_token 파싱으로 사용자 정보 획득")
    void shouldGetSocialTokenWithUserInfoSuccessfully() {
        // Given - id_token 페이로드 생성 (sub, nickname, picture 포함)
        String idTokenPayload = "eyJhdWQiOiJ0ZXN0LWNsaWVudC1pZCIsInN1YiI6IjEyMzQ1Njc4OSIsIm5pY2tuYW1lIjoi7YWM7Iqk7Yq4IOuLieuEpOyehCIsInBpY3R1cmUiOiJodHRwczovL2V4YW1wbGUuY29tL3Byb2ZpbGUuanBnIiwiaXNzIjoiaHR0cHM6Ly9rYXV0aC5rYWthby5jb20iLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDAwMzYwMH0";
        String idToken = "eyJhbGciOiJSUzI1NiJ9." + idTokenPayload + ".signature";

        Map<String, Object> tokenResponse = new java.util.HashMap<>();
        tokenResponse.put("access_token", TEST_ACCESS_TOKEN);
        tokenResponse.put("refresh_token", TEST_REFRESH_TOKEN);
        tokenResponse.put("id_token", idToken);

        given(kakaoAuthClient.getToken(anyString(), any(Map.class))).willReturn(tokenResponse);

        // When - getSocialToken 호출
        SocialMemberProfile result = kakaoAuthStrategy.getSocialToken(TEST_AUTH_CODE, null);

        // Then - 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getSocialId()).isEqualTo("123456789");
        assertThat(result.getEmail()).isNull();
        assertThat(result.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(result.getNickname()).isEqualTo("테스트 닉네임");
        assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
        assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);

        verify(kakaoAuthClient).getToken(anyString(), argThat(params -> {
            Map<String, String> expectedParams = (Map<String, String>) params;
            return expectedParams.get("grant_type").equals("authorization_code") &&
                   expectedParams.get("client_id").equals(KAKAO_CLIENT_ID) &&
                   expectedParams.get("code").equals(TEST_AUTH_CODE);
        }));
    }

    @Test
    @DisplayName("getSocialToken 실패 - 토큰 발급 실패 시 예외 발생")
    void shouldThrowExceptionWhenTokenRequestFails() {
        // Given
        given(kakaoAuthClient.getToken(anyString(), any(Map.class)))
            .willThrow(new RuntimeException("AuthToken request failed"));

        assertThatThrownBy(() -> kakaoAuthStrategy.getSocialToken(TEST_AUTH_CODE, null))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("unlink 성공 - 카카오 계정 연결 해제")
    void shouldUnlinkSuccessfully() {
        // Given
        // unlink는 void 메서드이므로 반환값이 없음
        doNothing().when(kakaoApiClient).unlink(anyString(), any(Map.class));

        kakaoAuthStrategy.unlink(TEST_SOCIAL_ID, TEST_ACCESS_TOKEN);

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

        assertThatThrownBy(() -> kakaoAuthStrategy.unlink(TEST_SOCIAL_ID, TEST_ACCESS_TOKEN))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("socialLogout 성공")
    void shouldLogoutSuccessfully() throws Exception {
        // Given
        doNothing().when(kakaoApiClient).logout(anyString());

        kakaoAuthStrategy.logout(TEST_ACCESS_TOKEN);

        verify(kakaoApiClient).logout(eq("Bearer " + TEST_ACCESS_TOKEN));
    }

    @Test
    @DisplayName("socialLogout 실패시 예외를 상위로 전파한다")
    void shouldPropagateExceptionWhenLogoutFails() {
        // Given
        RuntimeException expectedException = new RuntimeException("Logout failed");
        doThrow(expectedException)
            .when(kakaoApiClient).logout(anyString());

        assertThatThrownBy(() -> kakaoAuthStrategy.logout(TEST_ACCESS_TOKEN))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Logout failed");

        verify(kakaoApiClient).logout(anyString());
    }

    @Test
    @DisplayName("forceLogout 성공 - 카카오 강제 로그아웃")
    void shouldForceLogoutSuccessfully() {
        // Given
        doNothing().when(kakaoApiClient).forceLogout(anyString(), any(Map.class));

        // When
        kakaoAuthStrategy.forceLogout(TEST_SOCIAL_ID);

        // Then
        verify(kakaoApiClient).forceLogout(
            eq("KakaoAK " + KAKAO_ADMIN_KEY),
            argThat(params -> {
                Map<String, String> expectedParams = (Map<String, String>) params;
                return expectedParams.get("target_id_type").equals("user_id") &&
                       expectedParams.get("target_id").equals(TEST_SOCIAL_ID);
            })
        );
    }

    @Test
    @DisplayName("forceLogout 실패 시 예외 발생")
    void shouldThrowExceptionWhenForceLogoutFails() {
        // Given
        doThrow(new RuntimeException("Force logout failed"))
            .when(kakaoApiClient).forceLogout(anyString(), any(Map.class));

        // When & Then
        assertThatThrownBy(() -> kakaoAuthStrategy.forceLogout(TEST_SOCIAL_ID))
            .isInstanceOf(Exception.class);
    }
}
