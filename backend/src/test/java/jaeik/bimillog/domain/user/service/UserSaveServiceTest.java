package jaeik.bimillog.domain.user.service;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.user.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.application.service.UserSaveService;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.domain.user.entity.userdetail.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.userdetail.NewUserDetail;
import jaeik.bimillog.domain.user.entity.userdetail.UserDetail;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserSaveService 단위 테스트</h2>
 * <p>소셜 로그인 시 사용자 데이터 저장 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>기존 사용자와 신규 사용자 처리 로직을 중심으로 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("UserSaveService 단위 테스트")
@Tag("test")
class UserSaveServiceTest extends BaseUnitTest {

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private RedisUserDataPort redisUserDataPort;

    @InjectMocks
    private UserSaveService userSaveService;

    private SocialUserProfile testSocialProfile;
    private String testFcmToken;

    @BeforeEach
    protected void setUpChild() {
        testSocialProfile = new SocialUserProfile(
            "kakao123",
            "test@example.com",
            SocialProvider.KAKAO,
            "testNickname",
            "profile.jpg",
            "access-TemporaryToken",
            "refresh-TemporaryToken"
        );
        testFcmToken = "fcm-TemporaryToken-123";
    }

    @Test
    @DisplayName("기존 사용자 처리 - ExistingUserDetail 반환")
    void shouldReturnExistingUserDetail_WhenExistingUser() {
        // Given
        given(userQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.of(getTestUser()));

        ExistingUserDetail expectedDetail = ExistingUserDetail.of(getTestUser(), 1L, 100L);
        given(saveUserPort.handleExistingUserData(
            getTestUser(),
            testSocialProfile,
            testFcmToken
        )).willReturn(expectedDetail);

        // When
        UserDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );

        // Then
        assertThat(result).isInstanceOf(ExistingUserDetail.class);
        ExistingUserDetail existingUserDetail = (ExistingUserDetail) result;
        assertThat(existingUserDetail.getUserId()).isEqualTo(getTestUser().getId());
        assertThat(existingUserDetail.getTokenId()).isEqualTo(1L);
        assertThat(existingUserDetail.getFcmTokenId()).isEqualTo(100L);

        verify(userQueryPort).findByProviderAndSocialId(SocialProvider.KAKAO, "kakao123");
        verify(saveUserPort).handleExistingUserData(getTestUser(), testSocialProfile, testFcmToken);
        verify(redisUserDataPort, never()).saveTempData(any(), any(), any());
    }

    @Test
    @DisplayName("신규 사용자 처리 - NewUserDetail 반환")
    void shouldReturnNewUserDetail_WhenNewUser() {
        // Given
        given(userQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.empty());

        // When
        UserDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );

        // Then
        assertThat(result).isInstanceOf(NewUserDetail.class);
        NewUserDetail newUserDetail = (NewUserDetail) result;
        assertThat(newUserDetail.getUuid()).isNotNull();
        assertThat(newUserDetail.getUuid()).isNotEmpty();

        // Redis에 임시 데이터 저장 검증
        ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SocialUserProfile> profileCaptor = ArgumentCaptor.forClass(SocialUserProfile.class);
        ArgumentCaptor<String> fcmCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisUserDataPort).saveTempData(
            uuidCaptor.capture(),
            profileCaptor.capture(),
            fcmCaptor.capture()
        );

        assertThat(uuidCaptor.getValue()).isEqualTo(newUserDetail.getUuid());
        assertThat(profileCaptor.getValue()).isEqualTo(testSocialProfile);
        assertThat(fcmCaptor.getValue()).isEqualTo(testFcmToken);

        verify(saveUserPort, never()).handleExistingUserData(any(), any(), any());
    }

    @Test
    @DisplayName("FCM 토큰이 null인 경우 처리")
    void shouldProcessUserData_WhenFcmTokenIsNull() {
        // Given
        given(userQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.of(getTestUser()));

        ExistingUserDetail expectedDetail = ExistingUserDetail.of(getTestUser(), 1L, null);
        given(saveUserPort.handleExistingUserData(
            getTestUser(),
            testSocialProfile,
            null
        )).willReturn(expectedDetail);

        // When
        UserDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            null
        );

        // Then
        assertThat(result).isInstanceOf(ExistingUserDetail.class);
        ExistingUserDetail existingUserDetail = (ExistingUserDetail) result;
        assertThat(existingUserDetail.getFcmTokenId()).isNull();

        verify(saveUserPort).handleExistingUserData(getTestUser(), testSocialProfile, null);
    }

    @Test
    @DisplayName("신규 사용자 - FCM 토큰 없이 처리")
    void shouldHandleNewUser_WithoutFcmToken() {
        // Given
        given(userQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.empty());

        // When
        UserDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            null
        );

        // Then
        assertThat(result).isInstanceOf(NewUserDetail.class);
        NewUserDetail newUserDetail = (NewUserDetail) result;

        // Redis에 FCM 토큰 없이 저장되는지 검증
        ArgumentCaptor<String> fcmCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisUserDataPort).saveTempData(
            eq(newUserDetail.getUuid()),
            eq(testSocialProfile),
            fcmCaptor.capture()
        );

        assertThat(fcmCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("다른 소셜 프로바이더로 기존 사용자 조회")
    void shouldFindExistingUser_WithDifferentProvider() {
        // Given
        SocialUserProfile googleProfile = new SocialUserProfile(
            "google456",
            "google@example.com",
            SocialProvider.GOOGLE,
            "googleUser",
            "google-profile.jpg",
            "access-token",
            "refresh-token"
        );

        User googleUser = getOtherUser();
        given(userQueryPort.findByProviderAndSocialId(
            SocialProvider.GOOGLE,
            "google456"
        )).willReturn(Optional.of(googleUser));

        ExistingUserDetail expectedDetail = ExistingUserDetail.of(googleUser, 2L, 200L);
        given(saveUserPort.handleExistingUserData(
            googleUser,
            googleProfile,
            testFcmToken
        )).willReturn(expectedDetail);

        // When
        UserDetail result = userSaveService.processUserData(
            SocialProvider.GOOGLE,
            googleProfile,
            testFcmToken
        );

        // Then
        assertThat(result).isInstanceOf(ExistingUserDetail.class);
        verify(userQueryPort).findByProviderAndSocialId(SocialProvider.GOOGLE, "google456");
        verify(saveUserPort).handleExistingUserData(googleUser, googleProfile, testFcmToken);
    }
}