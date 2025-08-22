package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.out.SaveUserPort;
import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.TokenVO;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
    private TempDataPort tempDataPort;

    @Mock
    private SaveUserPort saveUserPort;

    @InjectMocks
    private SignUpService signUpService;

    private String testUserName;
    private String testUuid;
    private SocialLoginPort.SocialUserProfile testSocialProfile;
    private TokenVO testTokenVO;
    private TemporaryUserDataDTO testTempData;
    private List<ResponseCookie> testCookies;

    @BeforeEach
    void setUp() {
        testUserName = "testUser";
        testUuid = "test-uuid-123";
        
        testSocialProfile = new SocialLoginPort.SocialUserProfile("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg");
        testTokenVO = TokenVO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        
        testTempData = new TemporaryUserDataDTO(
                SocialLoginUserData.builder()
                        .socialId(testSocialProfile.socialId())
                        .email(testSocialProfile.email())
                        .provider(testSocialProfile.provider())
                        .nickname(testSocialProfile.nickname())
                        .profileImageUrl(testSocialProfile.profileImageUrl())
                        .build(),
                testTokenVO, 
                "fcm-token");
        
        testCookies = List.of(
                ResponseCookie.from("access_token", "access-token").build(),
                ResponseCookie.from("refresh_token", "refresh-token").build()
        );
    }

    @Test
    @DisplayName("유효한 임시 데이터로 회원 가입 성공")
    void shouldSignUp_WhenValidTemporaryData() {
        // Given
        given(tempDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
        given(saveUserPort.saveNewUser(
                eq(testUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        )).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        assertThat(result).hasSize(2);
        
        verify(tempDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(
                testUserName, 
                testUuid, 
                testSocialProfile,
                testTokenVO,
                "fcm-token"
        );
    }

    @Test
    @DisplayName("존재하지 않는 임시 데이터로 회원 가입 시 INVALID_TEMP_DATA 예외 발생")
    void shouldThrowException_WhenTemporaryDataNotFound() {
        // Given
        String nonExistentUuid = "non-existent-uuid";
        given(tempDataPort.getTempData(nonExistentUuid)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(testUserName, nonExistentUuid))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TEMP_DATA);

        verify(tempDataPort).getTempData(nonExistentUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(testUserName), 
                eq(nonExistentUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        );
    }

    @Test
    @DisplayName("FCM 토큰이 없는 임시 데이터로 회원 가입")
    void shouldSignUp_WhenTemporaryDataWithoutFcmToken() {
        // Given
        TemporaryUserDataDTO tempDataWithoutFcm = new TemporaryUserDataDTO(
                SocialLoginUserData.builder()
                        .socialId(testSocialProfile.socialId())
                        .email(testSocialProfile.email())
                        .provider(testSocialProfile.provider())
                        .nickname(testSocialProfile.nickname())
                        .profileImageUrl(testSocialProfile.profileImageUrl())
                        .build(),
                testTokenVO, null);
        
        given(tempDataPort.getTempData(testUuid)).willReturn(Optional.of(tempDataWithoutFcm));
        given(saveUserPort.saveNewUser(
                eq(testUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq(null)
        )).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        
        verify(tempDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(testUserName, testUuid, testSocialProfile, testTokenVO, null);
    }

    @Test
    @DisplayName("다양한 사용자 이름으로 회원 가입 테스트")
    void shouldSignUp_WithDifferentUserNames() {
        // Given
        String[] userNames = {"사용자1", "User2", "user_3", "user-4"};
        
        given(tempDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));

        for (String userName : userNames) {
            given(saveUserPort.saveNewUser(
                    eq(userName), 
                    eq(testUuid), 
                    eq(testSocialProfile), 
                    eq(testTokenVO),
                    eq("fcm-token")
            )).willReturn(testCookies);

            // When
            List<ResponseCookie> result = signUpService.signUp(userName, testUuid);

            // Then
            assertThat(result).isEqualTo(testCookies);
            verify(saveUserPort).saveNewUser(userName, testUuid, testSocialProfile, testTokenVO, "fcm-token");
        }
    }

    @Test
    @DisplayName("빈 문자열 사용자 이름으로 회원 가입")
    void shouldSignUp_WithEmptyUserName() {
        // Given
        String emptyUserName = "";
        given(tempDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
        given(saveUserPort.saveNewUser(
                eq(emptyUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        )).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(emptyUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        verify(saveUserPort).saveNewUser(emptyUserName, testUuid, testSocialProfile, testTokenVO, "fcm-token");
    }

    @Test
    @DisplayName("null 사용자 이름으로 회원 가입")
    void shouldSignUp_WithNullUserName() {
        // Given
        String nullUserName = null;
        given(tempDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
        given(saveUserPort.saveNewUser(
                eq(nullUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        )).willReturn(testCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(nullUserName, testUuid);

        // Then
        assertThat(result).isEqualTo(testCookies);
        verify(saveUserPort).saveNewUser(nullUserName, testUuid, testSocialProfile, testTokenVO, "fcm-token");
    }

    @Test
    @DisplayName("빈 쿠키 리스트 반환 테스트")
    void shouldReturnEmptyCookies_WhenSaveReturnsEmpty() {
        // Given
        List<ResponseCookie> emptyCookies = List.of();
        given(tempDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
        given(saveUserPort.saveNewUser(
                eq(testUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        )).willReturn(emptyCookies);

        // When
        List<ResponseCookie> result = signUpService.signUp(testUserName, testUuid);

        // Then
        assertThat(result).isEmpty();
        verify(saveUserPort).saveNewUser(testUserName, testUuid, testSocialProfile, testTokenVO, "fcm-token");
    }
}