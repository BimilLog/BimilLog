package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.user.application.port.in.UserSaveUseCase;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.NewUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserDetail;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>AuthToUserAdapter 단위 테스트</h2>
 * <p>Auth 도메인에서 User 도메인으로의 위임과 결과 변환을 검증하는 테스트</p>
 * <p>어댑터의 중개 역할만 테스트하며, 비즈니스 로직은 테스트하지 않음</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthToUserAdapter 단위 테스트")
class AuthToUserAdapterTest {

    @Mock
    private UserSaveUseCase userSaveUseCase;

    @InjectMocks
    private AuthToUserAdapter authToUserAdapter;

    private SocialUserProfile testSocialProfile;
    private Token testToken;
    private String testFcmToken;

    @BeforeEach
    void setUp() {
        testToken = Token.createTemporaryToken("access-token", "refresh-token");
        testSocialProfile = new SocialUserProfile(
            "kakao123",
            "test@example.com",
            SocialProvider.KAKAO,
            "testUser",
            "profile.jpg",
            testToken
        );
        testFcmToken = "fcm-token-123";
    }

    @Test
    @DisplayName("기존 사용자 - ExistingUserDetail 반환")
    void shouldReturnExistingUserDetail() {
        // Given
        User testUser = TestUsers.USER1;
        ExistingUserDetail existingUserDetail = ExistingUserDetail.of(testUser, 1L, 100L);

        given(userSaveUseCase.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        )).willReturn(existingUserDetail);

        // When
        UserDetail result = authToUserAdapter.delegateUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );

        // Then
        assertThat(result).isInstanceOf(ExistingUserDetail.class);
        ExistingUserDetail existingUser = (ExistingUserDetail) result;
        assertThat(existingUser.getUserId()).isEqualTo(testUser.getId());
        assertThat(existingUser.getTokenId()).isEqualTo(1L);
        assertThat(existingUser.getFcmTokenId()).isEqualTo(100L);

        verify(userSaveUseCase).processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );
    }

    @Test
    @DisplayName("신규 사용자 - NewUserDetail 반환")
    void shouldReturnNewUserDetail() {
        // Given
        String uuid = "test-uuid-12345";
        NewUserDetail newUserDetail = NewUserDetail.of(uuid);

        given(userSaveUseCase.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        )).willReturn(newUserDetail);

        // When
        UserDetail result = authToUserAdapter.delegateUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );

        // Then
        assertThat(result).isInstanceOf(NewUserDetail.class);
        NewUserDetail newUser = (NewUserDetail) result;
        assertThat(newUser.getUuid()).isEqualTo(uuid);

        verify(userSaveUseCase).processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );
    }

    @Test
    @DisplayName("FCM 토큰 없이 기존 사용자 처리")
    void shouldHandleExistingUserWithoutFcmToken() {
        // Given
        User testUser = TestUsers.USER1;
        ExistingUserDetail existingUserDetail = ExistingUserDetail.of(testUser, 1L, null);

        given(userSaveUseCase.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            null
        )).willReturn(existingUserDetail);

        // When
        UserDetail result = authToUserAdapter.delegateUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            null
        );

        // Then
        assertThat(result).isInstanceOf(ExistingUserDetail.class);
        ExistingUserDetail existingUser = (ExistingUserDetail) result;
        assertThat(existingUser.getFcmTokenId()).isNull();

        verify(userSaveUseCase).processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            null
        );
    }

    @Test
    @DisplayName("다른 소셜 프로바이더 처리")
    void shouldHandleDifferentSocialProvider() {
        // Given
        SocialUserProfile googleProfile = new SocialUserProfile(
            "google456",
            "google@example.com",
            SocialProvider.GOOGLE,
            "googleUser",
            "google-profile.jpg",
            testToken
        );

        User googleUser = TestUsers.USER2;
        ExistingUserDetail existingUserDetail = ExistingUserDetail.of(googleUser, 2L, 200L);

        given(userSaveUseCase.processUserData(
            SocialProvider.GOOGLE,
            googleProfile,
            testFcmToken
        )).willReturn(existingUserDetail);

        // When
        UserDetail result = authToUserAdapter.delegateUserData(
            SocialProvider.GOOGLE,
            googleProfile,
            testFcmToken
        );

        // Then
        assertThat(result).isInstanceOf(ExistingUserDetail.class);
        verify(userSaveUseCase).processUserData(
            SocialProvider.GOOGLE,
            googleProfile,
            testFcmToken
        );
    }

    @Test
    @DisplayName("사용자 데이터 처리 실패 시 예외 전파")
    void shouldPropagateException_WhenUserDataProcessingFails() {
        // Given
        RuntimeException expectedException = new RuntimeException("데이터 처리 실패");
        given(userSaveUseCase.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        )).willThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> authToUserAdapter.delegateUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("데이터 처리 실패");

        verify(userSaveUseCase).processUserData(
            SocialProvider.KAKAO,
            testSocialProfile,
            testFcmToken
        );
    }
}