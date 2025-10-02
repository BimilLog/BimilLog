package jaeik.bimillog.adapter.out.member;

import jaeik.bimillog.domain.auth.application.port.out.AuthTokenPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenCommandPort;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
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
@Tag("unit")
class SaveMemberAdapterTest extends BaseUnitTest {

    @Mock private AuthTokenPort authTokenPort;
    @Mock private GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;
    @Mock private MemberRepository memberRepository;
    @Mock private FcmUseCase fcmUseCase;

    @InjectMocks private SaveMemberAdapter saveMemberAdapter;

    @Test
    @DisplayName("기존 회원 로그인 처리 - 정상적인 업데이트 및 FCM 토큰 ID 반환")
    void shouldHandleExistingMemberLogin_WhenValidMemberDataProvided() {
        // Given: 기존 회원과 토큰 정보
        String fcmToken = "fcm-TemporaryToken-12345";
        SocialMemberProfile memberProfile = new SocialMemberProfile("123456789", "test@example.com", SocialProvider.KAKAO, "업데이트된닉네임", "https://updated-profile.jpg", "access-TemporaryToken", "refresh-TemporaryToken", fcmToken);

        Long fcmTokenId = 100L;
        Long tokenId = 1L;

        Member existingMember = createTestMemberWithId(1L);
        existingMember.updateMemberInfo("기존닉네임", "https://old-profile.jpg");

        // AuthToken은 JWT refreshToken만 관리 (초기에는 빈 문자열, SocialLoginService에서 실제 JWT 업데이트)
        AuthToken savedAuthToken = AuthToken.builder()
                .id(tokenId)
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();

        given(authTokenPort.save(any(AuthToken.class))).willReturn(savedAuthToken);
        given(fcmUseCase.registerFcmToken(existingMember, fcmToken)).willReturn(fcmTokenId);

        // When: 기존 회원 로그인 처리
        MemberDetail result = saveMemberAdapter.handleExistingUserData(existingMember, memberProfile);

        // Then: 회원 정보 업데이트 검증
        assertThat(existingMember.getSocialNickname()).isEqualTo("업데이트된닉네임");
        assertThat(existingMember.getThumbnailImage()).isEqualTo("https://updated-profile.jpg");

        // 카카오 토큰 업데이트 검증
        verify(globalKakaoTokenCommandPort).updateTokens(
            existingMember.getId(),
            "access-TemporaryToken",
            "refresh-TemporaryToken"
        );

        // 토큰이 저장되는지 검증
        ArgumentCaptor<AuthToken> tokenCaptor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenPort).save(tokenCaptor.capture());
        AuthToken capturedAuthToken = tokenCaptor.getValue();
        // AuthToken은 JWT refreshToken만 저장 (초기에는 빈 문자열)
        assertThat(capturedAuthToken.getRefreshToken()).isEqualTo("");
        assertThat(capturedAuthToken.getMember()).isEqualTo(existingMember);

        // FCM 토큰 등록 및 ID 반환 검증
        verify(fcmUseCase).registerFcmToken(existingMember, fcmToken);

        // 반환된 MemberDetail 검증
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(existingMember.getId());
        assertThat(result.getTokenId()).isEqualTo(tokenId);
        assertThat(result.getFcmTokenId()).isEqualTo(fcmTokenId);
    }

    @Test
    @DisplayName("기존 회원 로그인 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenExistingMemberHasNoFcmToken() {
        // Given: FCM 토큰이 없는 기존 회원 로그인
        SocialMemberProfile memberProfile = new SocialMemberProfile("123456789", "fcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://example.jpg", "access-TemporaryToken", "refresh-TemporaryToken", null);

        Member existingMember = createTestMemberWithId(1L);

        AuthToken savedAuthToken = AuthToken.builder()
                .id(1L)
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();

        given(authTokenPort.save(any(AuthToken.class))).willReturn(savedAuthToken);

        // When: FCM 토큰 없이 기존 회원 로그인 처리
        MemberDetail result = saveMemberAdapter.handleExistingUserData(existingMember, memberProfile);

        // Then: 카카오 토큰 업데이트 검증
        verify(globalKakaoTokenCommandPort).updateTokens(
            existingMember.getId(),
            "access-TemporaryToken",
            "refresh-TemporaryToken"
        );

        // FCM 토큰 등록이 호출되지 않았는지 검증
        verify(fcmUseCase, never()).registerFcmToken(any(), any());
        verify(authTokenPort).save(any(AuthToken.class));

        // FCM 토큰 ID가 null인지 검증
        assertThat(result.getFcmTokenId()).isNull();
    }

    @Test
    @DisplayName("신규 회원 저장 - 정상적인 저장 및 FCM 토큰 ID 반환")
    void shouldSaveNewMember_WhenValidDataProvided() {
        // Given: 신규 회원 저장 정보
        String memberName = "newMember";
        String fcmToken = "new-fcm-TemporaryToken";
        Long fcmTokenId = 200L;

        SocialMemberProfile memberProfile = new SocialMemberProfile("987654321", "newuser@example.com", SocialProvider.KAKAO, "신규사용자", "https://new-profile.jpg", "access-TemporaryToken", "refresh-TemporaryToken", fcmToken);

        Member newMember = TestMembers.copyWithId(getOtherMember(), 2L);

        // KakaoToken 모의 객체
        KakaoToken savedKakaoToken = KakaoToken.builder()
                .id(50L)
                .kakaoAccessToken("access-TemporaryToken")
                .kakaoRefreshToken("refresh-TemporaryToken")
                .build();

        AuthToken newAuthToken = AuthToken.builder()
                .id(1L)
                .refreshToken("")
                .member(newMember)
                .useCount(0)
                .build();

        given(globalKakaoTokenCommandPort.save(any(KakaoToken.class))).willReturn(savedKakaoToken);
        given(memberRepository.save(any(Member.class))).willReturn(newMember);
        given(authTokenPort.save(any(AuthToken.class))).willReturn(newAuthToken);
        given(fcmUseCase.registerFcmToken(newMember, fcmToken)).willReturn(fcmTokenId);

        // When: 신규 회원 저장
        MemberDetail result = saveMemberAdapter.saveNewMember(memberName, memberProfile);

        // Then: 카카오 토큰 저장 검증
        ArgumentCaptor<KakaoToken> kakaoTokenCaptor = ArgumentCaptor.forClass(KakaoToken.class);
        verify(globalKakaoTokenCommandPort).save(kakaoTokenCaptor.capture());
        KakaoToken capturedKakaoToken = kakaoTokenCaptor.getValue();
        assertThat(capturedKakaoToken.getKakaoAccessToken()).isEqualTo("access-TemporaryToken");
        assertThat(capturedKakaoToken.getKakaoRefreshToken()).isEqualTo("refresh-TemporaryToken");

        // 회원 저장 검증
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member capturedMember = memberCaptor.getValue();
        assertThat(capturedMember.getMemberName()).isEqualTo(memberName);
        assertThat(capturedMember.getSocialNickname()).isEqualTo("신규사용자");
        assertThat(capturedMember.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(capturedMember.getSocialId()).isEqualTo("987654321");

        // FCM 토큰 등록 및 ID 반환 검증
        verify(fcmUseCase).registerFcmToken(newMember, fcmToken);

        // 토큰 저장 검증
        verify(authTokenPort).save(any(AuthToken.class));

        // 반환된 MemberDetail 검증
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(newMember.getId());
        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getFcmTokenId()).isEqualTo(fcmTokenId);
    }

    @Test
    @DisplayName("신규 회원 저장 - FCM 토큰 없을 때 등록 미호출")
    void shouldNotPublishFcmEvent_WhenFcmTokenIsEmpty() {
        // Given: FCM 토큰이 없는 신규 회원
        String memberName = "memberWithoutFcm";

        SocialMemberProfile memberProfile = new SocialMemberProfile("111222333", "nofcm@example.com", SocialProvider.KAKAO, "FCM없음", "https://no-fcm.jpg", "access-TemporaryToken", "refresh-TemporaryToken", null);

        Member newMember = TestMembers.copyWithId(getThirdMember(), 3L);
        newMember.updateMemberInfo("FCM없음", "https://no-fcm.jpg");

        // KakaoToken 모의 객체
        KakaoToken savedKakaoToken = KakaoToken.builder()
                .id(60L)
                .kakaoAccessToken("access-TemporaryToken")
                .kakaoRefreshToken("refresh-TemporaryToken")
                .build();

        AuthToken newAuthToken = AuthToken.builder()
                .id(1L)
                .refreshToken("")
                .member(newMember)
                .useCount(0)
                .build();

        given(globalKakaoTokenCommandPort.save(any(KakaoToken.class))).willReturn(savedKakaoToken);
        given(memberRepository.save(any(Member.class))).willReturn(newMember);
        given(authTokenPort.save(any(AuthToken.class))).willReturn(newAuthToken);

        // When: FCM 토큰 없이 회원 저장
        MemberDetail result = saveMemberAdapter.saveNewMember(memberName, memberProfile);

        // Then: 카카오 토큰 저장 검증
        verify(globalKakaoTokenCommandPort).save(any(KakaoToken.class));

        // FCM 토큰이 null이므로 FCM 등록 호출되지 않음
        verify(fcmUseCase, never()).registerFcmToken(any(), any());

        // 반환된 ExistingMemberDetail의 FCM 토큰 ID가 null인지 검증
        assertThat(result.getFcmTokenId()).isNull();
    }

}