package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.application.service.SignUpService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>SignUpService 단위 테스트</h2>
 * <p>회원 가입 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpService 단위 테스트")
class SignUpServiceTest {

    @Mock
    private RedisUserDataPort redisUserDataPort;

    @Mock
    private SaveUserPort saveUserPort;

    @InjectMocks
    private SignUpService signUpService;

    private String testUserName;
    private String testUuid;
    private LoginResult.SocialUserProfile testSocialProfile;
    private Token testToken;
    private LoginResult.TempUserData testTempData;
    private List<ResponseCookie> testCookies;

    @BeforeEach
    void setUp() {
        testUserName = "testUser";
        testUuid = "test-uuid-123";
        
        testSocialProfile = new LoginResult.SocialUserProfile("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg");
        testToken = Token.createTemporaryToken("access-token", "refresh-token");
        
        testTempData = LoginResult.TempUserData.of(testSocialProfile, testToken, "fcm-token");
        
        testCookies = List.of(
                ResponseCookie.from("access_token", "access-token").build(),
                ResponseCookie.from("refresh_token", "refresh-token").build()
        );
    }

    @Test
    @DisplayName("유효한 임시 데이터로 회원 가입 성공")
    void shouldSignUp_WhenValidTemporaryData() {
        // Given
        given(redisUserDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
        given(saveUserPort.saveNewUser(
                eq(testUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testToken),
                eq("fcm-token")
        )).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        assertThat(result).hasSize(2);
        
        verify(redisUserDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(
                testUserName, 
                testUuid, 
                testSocialProfile,
                testToken,
                "fcm-token"
        );
    }

    @Test
    @DisplayName("존재하지 않는 임시 데이터로 회원 가입 시 INVALID_TEMP_DATA 예외 발생")
    void shouldThrowException_WhenTemporaryDataNotFound() {
        // Given
        String nonExistentUuid = "non-existent-uuid";
        given(redisUserDataPort.getTempData(nonExistentUuid)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(testUserName, nonExistentUuid))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.INVALID_TEMP_DATA);

        verify(redisUserDataPort).getTempData(nonExistentUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(testUserName), 
                eq(nonExistentUuid), 
                eq(testSocialProfile), 
                eq(testToken),
                eq("fcm-token")
        );
    }

    @Test
    @DisplayName("FCM 토큰이 없는 임시 데이터로 회원 가입")
    void shouldSignUp_WhenTemporaryDataWithoutFcmToken() {
        // Given
        LoginResult.TempUserData tempDataWithoutFcm = LoginResult.TempUserData.of(testSocialProfile, testToken, null);
        
        given(redisUserDataPort.getTempData(testUuid)).willReturn(Optional.of(tempDataWithoutFcm));
        given(saveUserPort.saveNewUser(
                eq(testUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testToken),
                eq(null)
        )).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        
        verify(redisUserDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(testUserName, testUuid, testSocialProfile, testToken, null);
    }







}