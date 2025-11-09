package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.in.GlobalFcmSaveUseCase;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.out.SaveMemberAdapter;
import jaeik.bimillog.infrastructure.redis.RedisMemberDataAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>MemberSignupService 단위 테스트</h2>
 * <p>신규 회원 가입 시 Redis 임시 데이터 조회부터 JWT 쿠키 발급까지의 흐름을 검증합니다.</p>
 */
@DisplayName("MemberSignupService 단위 테스트")
@Tag("unit")
class MemberSignupServiceTest extends BaseUnitTest {

    @Mock private RedisMemberDataAdapter redisMemberDataPort;
    @Mock private SaveMemberAdapter saveMemberPort;
    @Mock private GlobalCookiePort globalCookiePort;
    @Mock private GlobalJwtPort globalJwtPort;
    @Mock private GlobalAuthTokenSavePort globalAuthTokenSavePort;
    @Mock private GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;
    @Mock private GlobalFcmSaveUseCase globalFcmSaveUseCase;

    @InjectMocks private MemberSignupService signUpService;

    private SocialMemberProfile socialProfile;
    private Member persistedMember;
    private AuthToken persistedAuthToken;
    private List<ResponseCookie> jwtCookies;

    @BeforeEach
    void setUp() {
        socialProfile = new SocialMemberProfile(
                "kakao123",
                "signup@example.com",
                SocialProvider.KAKAO,
                "signupNickname",
                "profile.jpg",
                "access-token",
                "refresh-token",
                "fcm-token"
        );

        persistedMember = TestMembers.createMember("kakao123", "tester", "signupNickname");
        TestFixtures.setFieldValue(persistedMember, "id", 1L);
        if (persistedMember.getSetting() != null) {
            TestFixtures.setFieldValue(persistedMember.getSetting(), "id", 10L);
        }

        persistedAuthToken = AuthToken.builder()
                .id(99L)
                .member(persistedMember)
                .refreshToken("")
                .useCount(0)
                .build();

        jwtCookies = List.of(
                ResponseCookie.from("access_token", "access")
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .build(),
                ResponseCookie.from("refresh_token", "refresh")
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .build()
        );
    }

    @Test
    @DisplayName("유효한 임시 데이터로 회원 가입을 완료한다")
    void shouldSignupWithValidTemporaryData() {
        given(redisMemberDataPort.getTempData("uuid-123")).willReturn(Optional.of(socialProfile));
        given(globalKakaoTokenCommandPort.save(any(KakaoToken.class)))
                .willReturn(KakaoToken.createKakaoToken("access-token", "refresh-token"));
        given(saveMemberPort.saveNewMember(any(Member.class))).willReturn(persistedMember);
        given(globalAuthTokenSavePort.save(any(AuthToken.class))).willReturn(persistedAuthToken);
        given(globalFcmSaveUseCase.registerFcmToken(persistedMember, socialProfile.getFcmToken())).willReturn(123L);
        given(globalJwtPort.generateAccessToken(any(MemberDetail.class))).willReturn("access-jwt");
        given(globalJwtPort.generateRefreshToken(any(MemberDetail.class))).willReturn("refresh-jwt");
        given(globalCookiePort.generateJwtCookie("access-jwt", "refresh-jwt")).willReturn(jwtCookies);

        List<ResponseCookie> result = signUpService.signup("tester", "uuid-123");

        assertThat(result).isEqualTo(jwtCookies);
        verify(redisMemberDataPort).getTempData("uuid-123");
        verify(globalKakaoTokenCommandPort).save(any(KakaoToken.class));
        verify(saveMemberPort).saveNewMember(any(Member.class));
        verify(globalAuthTokenSavePort).save(any(AuthToken.class));
        verify(globalFcmSaveUseCase).registerFcmToken(persistedMember, socialProfile.getFcmToken());

        ArgumentCaptor<MemberDetail> detailCaptor = ArgumentCaptor.forClass(MemberDetail.class);
        verify(globalJwtPort).generateAccessToken(detailCaptor.capture());
        verify(globalJwtPort).generateRefreshToken(detailCaptor.getValue());

        verify(globalAuthTokenSavePort).updateJwtRefreshToken(persistedAuthToken.getId(), "refresh-jwt");
        verify(globalCookiePort).generateJwtCookie("access-jwt", "refresh-jwt");
        verify(redisMemberDataPort).removeTempData("uuid-123");

        MemberDetail capturedDetail = detailCaptor.getValue();
        assertThat(capturedDetail.getAuthTokenId()).isEqualTo(persistedAuthToken.getId());
        assertThat(capturedDetail.getFcmTokenId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("임시 데이터가 없으면 INVALID_TEMP_DATA 예외를 던진다")
    void shouldThrowExceptionWhenTempDataMissing() {
        given(redisMemberDataPort.getTempData("missing-uuid")).willReturn(Optional.empty());

        assertThatThrownBy(() -> signUpService.signup("tester", "missing-uuid"))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.INVALID_TEMP_DATA);

        verify(redisMemberDataPort).getTempData("missing-uuid");
        verify(saveMemberPort, never()).saveNewMember(any(Member.class));
    }

    @Test
    @DisplayName("FCM 토큰이 없어도 회원 가입은 정상 진행된다")
    void shouldSignupWithoutFcmToken() {
        SocialMemberProfile profileWithoutFcm = new SocialMemberProfile(
                "kakao123",
                "signup@example.com",
                SocialProvider.KAKAO,
                "signupNickname",
                "profile.jpg",
                "access-token",
                "refresh-token",
                null
        );

        given(redisMemberDataPort.getTempData("uuid-123")).willReturn(Optional.of(profileWithoutFcm));
        given(globalKakaoTokenCommandPort.save(any(KakaoToken.class)))
                .willReturn(KakaoToken.createKakaoToken("access-token", "refresh-token"));
        given(saveMemberPort.saveNewMember(any(Member.class))).willReturn(persistedMember);
        given(globalAuthTokenSavePort.save(any(AuthToken.class))).willReturn(persistedAuthToken);
        given(globalFcmSaveUseCase.registerFcmToken(persistedMember, null)).willReturn(null);
        given(globalJwtPort.generateAccessToken(any(MemberDetail.class))).willReturn("access-jwt");
        given(globalJwtPort.generateRefreshToken(any(MemberDetail.class))).willReturn("refresh-jwt");
        given(globalCookiePort.generateJwtCookie("access-jwt", "refresh-jwt")).willReturn(jwtCookies);

        List<ResponseCookie> result = signUpService.signup("tester", "uuid-123");

        assertThat(result).isEqualTo(jwtCookies);
        verify(globalFcmSaveUseCase).registerFcmToken(persistedMember, null);

        ArgumentCaptor<MemberDetail> detailCaptor = ArgumentCaptor.forClass(MemberDetail.class);
        verify(globalJwtPort).generateAccessToken(detailCaptor.capture());
        verify(globalJwtPort).generateRefreshToken(detailCaptor.getValue());

        MemberDetail capturedDetail = detailCaptor.getValue();
        assertThat(capturedDetail.getFcmTokenId()).isNull();
    }
}
