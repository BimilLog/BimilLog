package jaeik.bimillog.domain.user.service;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.user.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.user.application.service.SignUpService;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.TempUserData;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
@DisplayName("SignUpService 단위 테스트")
@Tag("test")
class SignUpServiceTest extends BaseUnitTest {

    @Mock
    private RedisUserDataPort redisUserDataPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private GlobalCookiePort globalCookiePort;

    @Mock
    private GlobalJwtPort globalJwtPort;

    @InjectMocks
    private SignUpService signUpService;

    private String testUserName;
    private String testUuid;
    private SocialUserProfile testSocialProfile;
    private TempUserData testTempData;
    private List<ResponseCookie> testCookies;
    private ExistingUserDetail testUserDetail;
    private final String testAccessToken = "test-access-TemporaryToken";
    private final String testRefreshToken = "test-refresh-TemporaryToken";

    @BeforeEach
    protected void setUpChild() {
        testUserName = "testUser";
        testUuid = "test-uuid-123";

        Token testToken = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        testSocialProfile = new SocialUserProfile("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg", testToken);

        testTempData = TempUserData.from(testSocialProfile, "fcm-TemporaryToken");
        
        testCookies = List.of(
                ResponseCookie.from("access_token", "access-TemporaryToken").build(),
                ResponseCookie.from("refresh_token", "refresh-TemporaryToken").build()
        );

        testUserDetail = ExistingUserDetail.of(getTestUser(), 1L, 100L);
    }

    @Test
    @DisplayName("유효한 임시 데이터로 회원 가입 성공")
    void shouldSignUp_WhenValidTemporaryData() {
        // Given
        given(redisUserDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
        given(saveUserPort.saveNewUser(
                eq(testUserName),
                eq(testSocialProfile),
                eq("fcm-TemporaryToken")
        )).willReturn(testUserDetail);
        given(globalJwtPort.generateAccessToken(testUserDetail)).willReturn(testAccessToken);
        given(globalJwtPort.generateRefreshToken(testUserDetail)).willReturn(testRefreshToken);
        given(globalCookiePort.generateJwtCookie(testAccessToken, testRefreshToken)).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        assertThat(result).hasSize(2);

        verify(redisUserDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(
                testUserName,
                testSocialProfile,
                "fcm-TemporaryToken"
        );
        verify(redisUserDataPort).removeTempData(testUuid);
        verify(globalJwtPort).generateAccessToken(testUserDetail);
        verify(globalJwtPort).generateRefreshToken(testUserDetail);
        verify(globalCookiePort).generateJwtCookie(testAccessToken, testRefreshToken);
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
        // saveNewUser should never be called
        verify(saveUserPort, never()).saveNewUser(
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("FCM 토큰이 없는 임시 데이터로 회원 가입")
    void shouldSignUp_WhenTemporaryDataWithoutFcmToken() {
        // Given
        TempUserData tempDataWithoutFcm = TempUserData.from(testSocialProfile, null);

        given(redisUserDataPort.getTempData(testUuid)).willReturn(Optional.of(tempDataWithoutFcm));
        given(saveUserPort.saveNewUser(
                eq(testUserName),
                eq(testSocialProfile),
                eq(null)
        )).willReturn(testUserDetail);
        given(globalJwtPort.generateAccessToken(testUserDetail)).willReturn(testAccessToken);
        given(globalJwtPort.generateRefreshToken(testUserDetail)).willReturn(testRefreshToken);
        given(globalCookiePort.generateJwtCookie(testAccessToken, testRefreshToken)).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);

        verify(redisUserDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(testUserName, testSocialProfile, null);
        verify(redisUserDataPort).removeTempData(testUuid);
        verify(globalJwtPort).generateAccessToken(testUserDetail);
        verify(globalJwtPort).generateRefreshToken(testUserDetail);
        verify(globalCookiePort).generateJwtCookie(testAccessToken, testRefreshToken);
    }







}