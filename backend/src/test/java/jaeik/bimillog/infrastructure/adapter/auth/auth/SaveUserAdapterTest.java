package jaeik.bimillog.infrastructure.adapter.auth.auth;

import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.user.application.port.out.TokenPort;
import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.SaveUserAdapter;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>SaveDataAdapter 단위 테스트</h2>
 * <p>비즈니스 로직과 외부 의존성 간 상호작용을 Mock으로 검증</p>
 * <p>트랜잭션 처리, 이벤트 발행, 예외 처리 로직을 중점 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SaveUserAdapterTest {

    @Mock private TokenPort tokenPort;
    @Mock private AuthCookieManager authCookieManager;
    @Mock private UserQueryPort userQueryPort;
    @Mock private UserCommandPort userCommandPort;
    @Mock private RedisUserDataPort redisUserDataPort;
    @Mock private NotificationFcmUseCase notificationFcmUseCase;

    @InjectMocks private SaveUserAdapter saveDataAdapter;

    @Test
    @DisplayName("기존 사용자 로그인 처리 - 정상적인 업데이트 및 이벤트 발행")
    void shouldHandleExistingUserLogin_WhenValidUserDataProvided() {
        // Given: 기존 사용자와 토큰 정보
        LoginResult.SocialUserProfile userProfile = new LoginResult.SocialUserProfile("123456789", "test@example.com", SocialProvider.KAKAO, "업데이트된닉네임", "https://updated-profile.jpg");

        Token tokenDTO = Token.createTemporaryToken("access-token", "refresh-token");

        String fcmToken = "fcm-token-12345";

        User existingUser = User.builder()
                .id(1L)
                .userName("existingUser")
                .socialNickname("기존닉네임")
                .thumbnailImage("https://old-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .setting(Setting.createSetting())
                .build();

        Token existingToken = Token.createTemporaryToken("access-token", "refresh-token");

        List<ResponseCookie> expectedCookies = List.of(
                ResponseCookie.from("jwt", "generated-jwt").build()
        );

        given(userQueryPort.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(existingUser);
        given(tokenPort.save(any(Token.class))).willReturn(existingToken);
        given(authCookieManager.generateJwtCookie(any(UserDetail.class))).willReturn(expectedCookies);

        // When: 기존 사용자 로그인 처리
        List<ResponseCookie> result = saveDataAdapter.handleExistingUserLogin(userProfile, tokenDTO, fcmToken);

        // Then: 사용자 정보 업데이트 검증
        assertThat(existingUser.getSocialNickname()).isEqualTo("업데이트된닉네임");
        assertThat(existingUser.getThumbnailImage()).isEqualTo("https://updated-profile.jpg");
        
        // 토큰이 저장되는지 검증
        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenPort).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getAccessToken()).isEqualTo("access-token");
        assertThat(savedToken.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(savedToken.getUsers()).isEqualTo(existingUser);
        
        // FCM 토큰 직접 등록 검증
        verify(notificationFcmUseCase).registerFcmToken(1L, fcmToken);
        
        // 쿠키 생성 결과 검증
        assertThat(result).isEqualTo(expectedCookies);
    }

    @Test
    @DisplayName("기존 사용자 로그인 - 사용자 미존재 시 예외 발생")
    void shouldThrowException_WhenUserNotFoundInExistingLogin() {
        // Given: 존재하지 않는 사용자 정보
        LoginResult.SocialUserProfile userProfile = new LoginResult.SocialUserProfile("nonexistent", "nonexistent@example.com", SocialProvider.KAKAO, "존재안함", "https://example.jpg");

        Token tokenDTO = Token.createTemporaryToken("access-token", "refresh-token");

        given(userQueryPort.findByProviderAndSocialId(SocialProvider.KAKAO, "nonexistent"))
                .willThrow(new jaeik.bimillog.domain.user.exception.UserCustomException(jaeik.bimillog.domain.user.exception.UserErrorCode.USER_NOT_FOUND));

        // When & Then: 예외 발생 검증
        assertThatThrownBy(() -> saveDataAdapter.handleExistingUserLogin(userProfile, tokenDTO, null))
                .isInstanceOf(jaeik.bimillog.domain.user.exception.UserCustomException.class)
                .hasMessage(jaeik.bimillog.domain.user.exception.UserErrorCode.USER_NOT_FOUND.getMessage());

        // 후속 작업이 실행되지 않았는지 검증
        verify(tokenPort, never()).save(any());
        verify(notificationFcmUseCase, never()).registerFcmToken(any(), any());
    }

    @Test
    @DisplayName("기존 사용자 로그인 - FCM 토큰 없을 때 등록 미호출")  
    void shouldNotPublishFcmEvent_WhenExistingUserHasNoFcmToken() {
        // Given: FCM 토큰이 없는 기존 사용자 로그인
        LoginResult.SocialUserProfile userProfile = new LoginResult.SocialUserProfile("123456789", "fcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://example.jpg");

        Token tokenDTO = Token.createTemporaryToken("access-token", "refresh-token");

        User existingUser = User.builder()
                .id(1L)
                .userName("existingUser")
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .setting(Setting.createSetting())
                .build();

        Token savedToken = Token.createTemporaryToken("access-token", "refresh-token");
                

        given(userQueryPort.findByProviderAndSocialId(SocialProvider.KAKAO, "123456789"))
                .willReturn(existingUser);
        given(tokenPort.save(any(Token.class))).willReturn(savedToken);
        given(authCookieManager.generateJwtCookie(any(UserDetail.class))).willReturn(List.of());

        // When: FCM 토큰 없이 기존 사용자 로그인 처리
        saveDataAdapter.handleExistingUserLogin(userProfile, tokenDTO, null);
        
        // Then: FCM 토큰 등록이 호출되지 않았는지 검증
        verify(notificationFcmUseCase, never()).registerFcmToken(any(), any());

        // Then: FCM 이벤트가 발행되지 않았는지 검증
        verify(notificationFcmUseCase, never()).registerFcmToken(any(), any());
        verify(tokenPort).save(any(Token.class));
    }

    @Test
    @DisplayName("신규 사용자 저장 - 정상적인 저장 및 이벤트 발행")
    void shouldSaveNewUser_WhenValidDataProvided() {
        // Given: 신규 사용자 저장 정보
        String userName = "newUser";
        String uuid = "temp-uuid-12345";
        String fcmToken = "new-fcm-token";
        
        LoginResult.SocialUserProfile userProfile = new LoginResult.SocialUserProfile("987654321", "newuser@example.com", SocialProvider.KAKAO, "신규사용자", "https://new-profile.jpg");

        Token tokenDTO = Token.createTemporaryToken("access-token", "refresh-token");

        User newUser = User.builder()
                .id(2L)
                .userName(userName)
                .socialNickname(userProfile.nickname())
                .thumbnailImage(userProfile.profileImageUrl())
                .provider(userProfile.provider())
                .socialId(userProfile.socialId())
                .setting(Setting.createSetting())
                .build();

        Token newToken = Token.createTemporaryToken("access-token", "refresh-token");

        List<ResponseCookie> expectedCookies = List.of(
                ResponseCookie.from("jwt", "new-user-jwt").build()
        );

        given(userCommandPort.save(any(User.class))).willReturn(newUser);
        given(tokenPort.save(any(Token.class))).willReturn(newToken);
        given(authCookieManager.generateJwtCookie(any(UserDetail.class))).willReturn(expectedCookies);

        // When: 신규 사용자 저장
        List<ResponseCookie> result = saveDataAdapter.saveNewUser(userName, uuid, userProfile, tokenDTO, fcmToken);

        // Then: 사용자 저장 검증
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userCommandPort).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUserName()).isEqualTo(userName);
        assertThat(capturedUser.getSocialNickname()).isEqualTo("신규사용자");
        assertThat(capturedUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(capturedUser.getSocialId()).isEqualTo("987654321");

        // FCM 토큰 직접 등록 검증
        verify(notificationFcmUseCase).registerFcmToken(2L, fcmToken);

        // 임시 데이터 삭제 검증
        verify(redisUserDataPort).removeTempData(uuid);
        
        // 토큰 저장 및 쿠키 결과 검증
        verify(tokenPort).save(any(Token.class));
        assertThat(result).isEqualTo(expectedCookies);
    }

    @Test
    @DisplayName("신규 사용자 저장 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenFcmTokenIsEmpty() {
        // Given: FCM 토큰이 없는 신규 사용자
        String userName = "userWithoutFcm";
        String uuid = "temp-uuid-67890";
        String fcmToken = null; // FCM 토큰 없음
        
        LoginResult.SocialUserProfile userProfile = new LoginResult.SocialUserProfile("111222333", "nofcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://no-fcm.jpg");

        Token tokenDTO = Token.createTemporaryToken("access-token", "refresh-token");

        User newUser = User.builder()
                .id(3L)
                .userName(userName)
                .setting(Setting.createSetting())
                .build();

        Token newToken = Token.createTemporaryToken("access-token", "refresh-token");

        given(userCommandPort.save(any(User.class))).willReturn(newUser);
        given(tokenPort.save(any(Token.class))).willReturn(newToken);
        given(authCookieManager.generateJwtCookie(any(UserDetail.class))).willReturn(List.of());

        // When: FCM 토큰 없이 사용자 저장
        saveDataAdapter.saveNewUser(userName, uuid, userProfile, tokenDTO, fcmToken);

        // Then: FCM 토큰이 null이므로 FCM 등록 호출되지 않음
        verify(notificationFcmUseCase, never()).registerFcmToken(any(), any());
    }

}