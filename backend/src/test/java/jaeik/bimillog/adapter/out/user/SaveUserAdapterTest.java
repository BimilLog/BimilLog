package jaeik.bimillog.adapter.out.user;

import jaeik.bimillog.domain.auth.application.port.out.TokenCommandPort;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.domain.user.entity.userdetail.ExistingUserDetail;
import jaeik.bimillog.infrastructure.adapter.out.user.SaveUserAdapter;
import jaeik.bimillog.infrastructure.adapter.out.user.UserRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>SaveUserAdapter 단위 테스트</h2>
 * <p>비즈니스 로직과 외부 의존성 간 상호작용을 Mock으로 검증</p>
 * <p>트랜잭션 처리, 이벤트 발행, 예외 처리 로직을 중점 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Tag("test")
class SaveUserAdapterTest extends BaseUnitTest {

    @Mock private TokenCommandPort tokenCommandPort;
    @Mock private UserRepository userRepository;
    @Mock private FcmUseCase fcmUseCase;

    @InjectMocks private SaveUserAdapter saveUserAdapter;

    @Test
    @DisplayName("기존 사용자 로그인 처리 - 정상적인 업데이트 및 FCM 토큰 ID 반환")
    void shouldHandleExistingUserLogin_WhenValidUserDataProvided() {
        // Given: 기존 사용자와 토큰 정보
        String fcmToken = "fcm-TemporaryToken-12345";
        SocialUserProfile userProfile = new SocialUserProfile("123456789", "test@example.com", SocialProvider.KAKAO, "업데이트된닉네임", "https://updated-profile.jpg", "access-TemporaryToken", "refresh-TemporaryToken", fcmToken);

        Long fcmTokenId = 100L;
        Long tokenId = 1L;

        User existingUser = createTestUserWithId(1L);
        existingUser.updateUserInfo("기존닉네임", "https://old-profile.jpg");

        Token savedToken = Token.builder()
                .id(tokenId)
                .accessToken("access-TemporaryToken")
                .refreshToken("refresh-TemporaryToken")
                .users(existingUser)
                .build();

        given(tokenCommandPort.save(any(Token.class))).willReturn(savedToken);
        given(fcmUseCase.registerFcmToken(existingUser, fcmToken)).willReturn(fcmTokenId);

        // When: 기존 사용자 로그인 처리
        ExistingUserDetail result = saveUserAdapter.handleExistingUserData(existingUser, userProfile);

        // Then: 사용자 정보 업데이트 검증
        assertThat(existingUser.getSocialNickname()).isEqualTo("업데이트된닉네임");
        assertThat(existingUser.getThumbnailImage()).isEqualTo("https://updated-profile.jpg");

        // 토큰이 저장되는지 검증
        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenCommandPort).save(tokenCaptor.capture());
        Token capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getAccessToken()).isEqualTo("access-TemporaryToken");
        assertThat(capturedToken.getRefreshToken()).isEqualTo("refresh-TemporaryToken");
        assertThat(capturedToken.getUsers()).isEqualTo(existingUser);

        // FCM 토큰 등록 및 ID 반환 검증
        verify(fcmUseCase).registerFcmToken(existingUser, fcmToken);

        // 반환된 ExistingUserDetail 검증
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(existingUser.getId());
        assertThat(result.getTokenId()).isEqualTo(tokenId);
        assertThat(result.getFcmTokenId()).isEqualTo(fcmTokenId);
    }

    @Test
    @DisplayName("기존 사용자 로그인 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenExistingUserHasNoFcmToken() {
        // Given: FCM 토큰이 없는 기존 사용자 로그인
        SocialUserProfile userProfile = new SocialUserProfile("123456789", "fcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://example.jpg", "access-TemporaryToken", "refresh-TemporaryToken", null);

        User existingUser = createTestUserWithId(1L);

        Token savedToken = Token.builder()
                .id(1L)
                .accessToken("access-TemporaryToken")
                .refreshToken("refresh-TemporaryToken")
                .users(existingUser)
                .build();

        given(tokenCommandPort.save(any(Token.class))).willReturn(savedToken);

        // When: FCM 토큰 없이 기존 사용자 로그인 처리
        ExistingUserDetail result = saveUserAdapter.handleExistingUserData(existingUser, userProfile);

        // Then: FCM 토큰 등록이 호출되지 않았는지 검증
        verify(fcmUseCase, never()).registerFcmToken(any(), any());
        verify(tokenCommandPort).save(any(Token.class));

        // FCM 토큰 ID가 null인지 검증
        assertThat(result.getFcmTokenId()).isNull();
    }

    @Test
    @DisplayName("신규 사용자 저장 - 정상적인 저장 및 FCM 토큰 ID 반환")
    void shouldSaveNewUser_WhenValidDataProvided() {
        // Given: 신규 사용자 저장 정보
        String userName = "newUser";
        String fcmToken = "new-fcm-TemporaryToken";
        Long fcmTokenId = 200L;

        SocialUserProfile userProfile = new SocialUserProfile("987654321", "newuser@example.com", SocialProvider.KAKAO, "신규사용자", "https://new-profile.jpg", "access-TemporaryToken", "refresh-TemporaryToken", fcmToken);

        User newUser = TestUsers.copyWithId(getOtherUser(), 2L);

        Token newToken = Token.builder()
                .id(1L)
                .accessToken("access-TemporaryToken")
                .refreshToken("refresh-TemporaryToken")
                .users(newUser)
                .build();

        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(tokenCommandPort.save(any(Token.class))).willReturn(newToken);
        given(fcmUseCase.registerFcmToken(newUser, fcmToken)).willReturn(fcmTokenId);

        // When: 신규 사용자 저장
        ExistingUserDetail result = saveUserAdapter.saveNewUser(userName, userProfile);

        // Then: 사용자 저장 검증
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUserName()).isEqualTo(userName);
        assertThat(capturedUser.getSocialNickname()).isEqualTo("신규사용자");
        assertThat(capturedUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(capturedUser.getSocialId()).isEqualTo("987654321");

        // FCM 토큰 등록 및 ID 반환 검증
        verify(fcmUseCase).registerFcmToken(newUser, fcmToken);

        // 토큰 저장 검증
        verify(tokenCommandPort).save(any(Token.class));

        // 반환된 ExistingUserDetail 검증
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(newUser.getId());
        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getFcmTokenId()).isEqualTo(fcmTokenId);
    }

    @Test
    @DisplayName("신규 사용자 저장 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenFcmTokenIsEmpty() {
        // Given: FCM 토큰이 없는 신규 사용자
        String userName = "userWithoutFcm";

        SocialUserProfile userProfile = new SocialUserProfile("111222333", "nofcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://no-fcm.jpg", "access-TemporaryToken", "refresh-TemporaryToken", null);

        User newUser = TestUsers.copyWithId(getThirdUser(), 3L);
        newUser.updateUserInfo("FCM없음", "https://no-fcm.jpg");

        Token newToken = Token.builder()
                .id(1L)
                .accessToken("access-TemporaryToken")
                .refreshToken("refresh-TemporaryToken")
                .users(newUser)
                .build();

        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(tokenCommandPort.save(any(Token.class))).willReturn(newToken);

        // When: FCM 토큰 없이 사용자 저장
        ExistingUserDetail result = saveUserAdapter.saveNewUser(userName, userProfile);

        // Then: FCM 토큰이 null이므로 FCM 등록 호출되지 않음
        verify(fcmUseCase, never()).registerFcmToken(any(), any());

        // 반환된 ExistingUserDetail의 FCM 토큰 ID가 null인지 검증
        assertThat(result.getFcmTokenId()).isNull();
    }

}