package jaeik.bimillog.adapter.out.member;

import jaeik.bimillog.domain.auth.application.port.out.AuthTokenPort;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.entity.memberdetail.ExistingMemberDetail;
import jaeik.bimillog.infrastructure.adapter.out.member.SaveMemberAdapter;
import jaeik.bimillog.infrastructure.adapter.out.member.MemberRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
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
 * <h2>SaveMemberAdapter 단위 테스트</h2>
 * <p>비즈니스 로직과 외부 의존성 간 상호작용을 Mock으로 검증</p>
 * <p>트랜잭션 처리, 이벤트 발행, 예외 처리 로직을 중점 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Tag("test")
class SaveMemberAdapterTest extends BaseUnitTest {

    @Mock private AuthTokenPort authTokenPort;
    @Mock private MemberRepository userRepository;
    @Mock private FcmUseCase fcmUseCase;

    @InjectMocks private SaveMemberAdapter saveUserAdapter;

    @Test
    @DisplayName("기존 사용자 로그인 처리 - 정상적인 업데이트 및 FCM 토큰 ID 반환")
    void shouldHandleExistingUserLogin_WhenValidUserDataProvided() {
        // Given: 기존 사용자와 토큰 정보
        String fcmToken = "fcm-TemporaryToken-12345";
        SocialMemberProfile userProfile = new SocialMemberProfile("123456789", "test@example.com", SocialProvider.KAKAO, "업데이트된닉네임", "https://updated-profile.jpg", "access-TemporaryToken", "refresh-TemporaryToken", fcmToken);

        Long fcmTokenId = 100L;
        Long tokenId = 1L;

        Member existingMember = createTestUserWithId(1L);
        existingMember.updateUserInfo("기존닉네임", "https://old-profile.jpg");

        // AuthToken은 JWT refreshToken만 관리 (초기에는 빈 문자열, SocialLoginService에서 실제 JWT 업데이트)
        AuthToken savedAuthToken = AuthToken.builder()
                .id(tokenId)
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();

        given(authTokenPort.save(any(AuthToken.class))).willReturn(savedAuthToken);
        given(fcmUseCase.registerFcmToken(existingMember, fcmToken)).willReturn(fcmTokenId);

        // When: 기존 사용자 로그인 처리
        ExistingMemberDetail result = saveUserAdapter.handleExistingUserData(existingMember, userProfile);

        // Then: 사용자 정보 업데이트 검증
        assertThat(existingMember.getSocialNickname()).isEqualTo("업데이트된닉네임");
        assertThat(existingMember.getThumbnailImage()).isEqualTo("https://updated-profile.jpg");

        // 토큰이 저장되는지 검증
        ArgumentCaptor<AuthToken> tokenCaptor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenPort).save(tokenCaptor.capture());
        AuthToken capturedAuthToken = tokenCaptor.getValue();
        // AuthToken은 JWT refreshToken만 저장 (초기에는 빈 문자열)
        assertThat(capturedAuthToken.getRefreshToken()).isEqualTo("");
        assertThat(capturedAuthToken.getMember()).isEqualTo(existingMember);

        // FCM 토큰 등록 및 ID 반환 검증
        verify(fcmUseCase).registerFcmToken(existingMember, fcmToken);

        // 반환된 ExistingMemberDetail 검증
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(existingMember.getId());
        assertThat(result.getTokenId()).isEqualTo(tokenId);
        assertThat(result.getFcmTokenId()).isEqualTo(fcmTokenId);
    }

    @Test
    @DisplayName("기존 사용자 로그인 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenExistingUserHasNoFcmToken() {
        // Given: FCM 토큰이 없는 기존 사용자 로그인
        SocialMemberProfile userProfile = new SocialMemberProfile("123456789", "fcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://example.jpg", "access-TemporaryToken", "refresh-TemporaryToken", null);

        Member existingMember = createTestUserWithId(1L);

        AuthToken savedAuthToken = AuthToken.builder()
                .id(1L)
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();

        given(authTokenPort.save(any(AuthToken.class))).willReturn(savedAuthToken);

        // When: FCM 토큰 없이 기존 사용자 로그인 처리
        ExistingMemberDetail result = saveUserAdapter.handleExistingUserData(existingMember, userProfile);

        // Then: FCM 토큰 등록이 호출되지 않았는지 검증
        verify(fcmUseCase, never()).registerFcmToken(any(), any());
        verify(authTokenPort).save(any(AuthToken.class));

        // FCM 토큰 ID가 null인지 검증
        assertThat(result.getFcmTokenId()).isNull();
    }

    @Test
    @DisplayName("신규 사용자 저장 - 정상적인 저장 및 FCM 토큰 ID 반환")
    void shouldSaveNewUser_WhenValidDataProvided() {
        // Given: 신규 사용자 저장 정보
        String userName = "newMember";
        String fcmToken = "new-fcm-TemporaryToken";
        Long fcmTokenId = 200L;

        SocialMemberProfile userProfile = new SocialMemberProfile("987654321", "newuser@example.com", SocialProvider.KAKAO, "신규사용자", "https://new-profile.jpg", "access-TemporaryToken", "refresh-TemporaryToken", fcmToken);

        Member newMember = TestMembers.copyWithId(getOtherUser(), 2L);

        AuthToken newAuthToken = AuthToken.builder()
                .id(1L)
                .accessToken("access-TemporaryToken")
                .refreshToken("refresh-TemporaryToken")
                .users(newMember)
                .build();

        given(userRepository.save(any(Member.class))).willReturn(newMember);
        given(authTokenPort.save(any(AuthToken.class))).willReturn(newAuthToken);
        given(fcmUseCase.registerFcmToken(newMember, fcmToken)).willReturn(fcmTokenId);

        // When: 신규 사용자 저장
        ExistingMemberDetail result = saveUserAdapter.saveNewUser(userName, userProfile);

        // Then: 사용자 저장 검증
        ArgumentCaptor<Member> userCaptor = ArgumentCaptor.forClass(Member.class);
        verify(userRepository).save(userCaptor.capture());
        Member capturedMember = userCaptor.getValue();
        assertThat(capturedMember.getUserName()).isEqualTo(userName);
        assertThat(capturedMember.getSocialNickname()).isEqualTo("신규사용자");
        assertThat(capturedMember.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(capturedMember.getSocialId()).isEqualTo("987654321");

        // FCM 토큰 등록 및 ID 반환 검증
        verify(fcmUseCase).registerFcmToken(newMember, fcmToken);

        // 토큰 저장 검증
        verify(authTokenPort).save(any(AuthToken.class));

        // 반환된 ExistingMemberDetail 검증
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(newMember.getId());
        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getFcmTokenId()).isEqualTo(fcmTokenId);
    }

    @Test
    @DisplayName("신규 사용자 저장 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenFcmTokenIsEmpty() {
        // Given: FCM 토큰이 없는 신규 사용자
        String userName = "userWithoutFcm";

        SocialMemberProfile userProfile = new SocialMemberProfile("111222333", "nofcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://no-fcm.jpg", "access-TemporaryToken", "refresh-TemporaryToken", null);

        Member newMember = TestMembers.copyWithId(getThirdUser(), 3L);
        newMember.updateUserInfo("FCM없음", "https://no-fcm.jpg");

        AuthToken newAuthToken = AuthToken.builder()
                .id(1L)
                .refreshToken("")
                .member(newMember)
                .useCount(0)
                .build();

        given(userRepository.save(any(Member.class))).willReturn(newMember);
        given(authTokenPort.save(any(AuthToken.class))).willReturn(newAuthToken);

        // When: FCM 토큰 없이 사용자 저장
        ExistingMemberDetail result = saveUserAdapter.saveNewUser(userName, userProfile);

        // Then: FCM 토큰이 null이므로 FCM 등록 호출되지 않음
        verify(fcmUseCase, never()).registerFcmToken(any(), any());

        // 반환된 ExistingUserDetail의 FCM 토큰 ID가 null인지 검증
        assertThat(result.getFcmTokenId()).isNull();
    }

}