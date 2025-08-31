package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.entity.TempUserData;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.TokenVO;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
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
    private SocialLoginPort.SocialUserProfile testSocialProfile;
    private TokenVO testTokenVO;
    private TempUserData testTempData;
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
        
        testTempData = TempUserData.of(testSocialProfile, testTokenVO, "fcm-token");
        
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
                eq(testTokenVO),
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
                testTokenVO,
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
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TEMP_DATA);

        verify(redisUserDataPort).getTempData(nonExistentUuid);
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
        TempUserData tempDataWithoutFcm = TempUserData.of(testSocialProfile, testTokenVO, null);
        
        given(redisUserDataPort.getTempData(testUuid)).willReturn(Optional.of(tempDataWithoutFcm));
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
        
        verify(redisUserDataPort).getTempData(testUuid);
        verify(saveUserPort).saveNewUser(testUserName, testUuid, testSocialProfile, testTokenVO, null);
    }

    @Test
    @DisplayName("다양한 사용자 이름으로 회원 가입 테스트")
    void shouldSignUp_WithDifferentUserNames() {
        // TODO: 테스트 설계 개선
        // 기존: 모든 사용자명에 같은 FCM 토큰 재사용하는 비논리적 설계
        // 수정: 각 사용자별로 고유한 UUID와 임시 데이터 사용
        
        // Given
        String[] userNames = {"사용자1", "User2", "user_3", "user-4"};
        String[] uniqueUuids = {"uuid-1", "uuid-2", "uuid-3", "uuid-4"};
        String[] uniqueFcmTokens = {"fcm-token-1", "fcm-token-2", "fcm-token-3", "fcm-token-4"};

        for (int i = 0; i < userNames.length; i++) {
            String userName = userNames[i];
            String uniqueUuid = uniqueUuids[i];
            String uniqueFcmToken = uniqueFcmTokens[i];
            
            // 각 사용자별로 고유한 임시 데이터 생성
            SocialLoginPort.SocialUserProfile uniqueProfile = new SocialLoginPort.SocialUserProfile(
                    "kakao" + (i + 1),
                    "test" + (i + 1) + "@example.com", 
                    testSocialProfile.provider(),
                    "testUser" + (i + 1),
                    "profile" + (i + 1) + ".jpg"
            );
            TempUserData uniqueTempData = TempUserData.of(uniqueProfile, testTokenVO, uniqueFcmToken);
            
            given(redisUserDataPort.getTempData(uniqueUuid)).willReturn(Optional.of(uniqueTempData));
            given(saveUserPort.saveNewUser(
                    eq(userName), 
                    eq(uniqueUuid), 
                    eq(uniqueTempData.userProfile()), 
                    eq(testTokenVO),
                    eq(uniqueFcmToken)
            )).willReturn(testCookies);

            // When
            List<ResponseCookie> result = signUpService.signUp(userName, uniqueUuid);

            // Then
            assertThat(result).isEqualTo(testCookies);
            verify(redisUserDataPort).getTempData(uniqueUuid);
            verify(saveUserPort).saveNewUser(userName, uniqueUuid, uniqueTempData.userProfile(), testTokenVO, uniqueFcmToken);
        }
    }

    @Test
    @DisplayName("빈 문자열 사용자 이름으로 회원 가입 시 INVALID_INPUT_VALUE 예외 발생")
    void shouldThrowException_WhenEmptyUserName() {
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // 기존: 빈 문자열이 성공하는 비논리적 테스트
        // 수정: 올바른 입력 검증으로 예외 발생 확인
        
        // Given
        String emptyUserName = "";

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(emptyUserName, testUuid))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        // 입력 검증 실패로 인해 tempDataPort나 saveUserPort는 호출되지 않아야 함
        verify(redisUserDataPort, never()).getTempData(testUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(emptyUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        );
    }

    @Test
    @DisplayName("null 사용자 이름으로 회원 가입 시 INVALID_INPUT_VALUE 예외 발생")
    void shouldThrowException_WhenNullUserName() {
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // 기존: null userName이 성공하는 비논리적 테스트
        // 수정: 올바른 입력 검증으로 예외 발생 확인
        
        // Given
        String nullUserName = null;

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(nullUserName, testUuid))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        // 입력 검증 실패로 인해 tempDataPort나 saveUserPort는 호출되지 않아야 함
        verify(redisUserDataPort, never()).getTempData(testUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(nullUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        );
    }

    @Test
    @DisplayName("빈 쿠키 리스트 반환 테스트")
    void shouldReturnEmptyCookies_WhenSaveReturnsEmpty() {
        // Given
        List<ResponseCookie> emptyCookies = List.of();
        given(redisUserDataPort.getTempData(testUuid)).willReturn(Optional.of(testTempData));
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

    @Test
    @DisplayName("null UUID로 회원 가입 시 INVALID_TEMP_UUID 예외 발생")
    void shouldThrowException_WhenNullUuid() {
        // Given
        String nullUuid = null;

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(testUserName, nullUuid))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TEMP_UUID);

        // 입력 검증 실패로 인해 tempDataPort나 saveUserPort는 호출되지 않아야 함
        verify(redisUserDataPort, never()).getTempData(nullUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(testUserName), 
                eq(nullUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        );
    }

    @Test
    @DisplayName("빈 문자열 UUID로 회원 가입 시 INVALID_TEMP_UUID 예외 발생")
    void shouldThrowException_WhenEmptyUuid() {
        // Given
        String emptyUuid = "";

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(testUserName, emptyUuid))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TEMP_UUID);

        // 입력 검증 실패로 인해 tempDataPort나 saveUserPort는 호출되지 않아야 함
        verify(redisUserDataPort, never()).getTempData(emptyUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(testUserName), 
                eq(emptyUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        );
    }

    @Test
    @DisplayName("공백만 있는 사용자 이름으로 회원 가입 시 INVALID_INPUT_VALUE 예외 발생")
    void shouldThrowException_WhenWhitespaceOnlyUserName() {
        // Given
        String whitespaceUserName = "   ";

        // When & Then
        assertThatThrownBy(() -> signUpService.signUp(whitespaceUserName, testUuid))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        // 입력 검증 실패로 인해 tempDataPort나 saveUserPort는 호출되지 않아야 함
        verify(redisUserDataPort, never()).getTempData(testUuid);
        verify(saveUserPort, never()).saveNewUser(
                eq(whitespaceUserName), 
                eq(testUuid), 
                eq(testSocialProfile), 
                eq(testTokenVO),
                eq("fcm-token")
        );
    }
}