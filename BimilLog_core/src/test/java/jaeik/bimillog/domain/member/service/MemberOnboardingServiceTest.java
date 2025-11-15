package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
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

@DisplayName("회원 온보딩 서비스 가입 흐름")
@Tag("unit")
class MemberOnboardingServiceTest extends BaseUnitTest {

    @Mock private RedisMemberDataAdapter redisMemberDataAdapter;
    @Mock private MemberRepository memberRepository;
    @Mock private GlobalCookieAdapter globalCookieAdapter;
    @Mock private GlobalJwtAdapter globalJwtAdapter;
    @Mock private GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;
    @Mock private GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    @InjectMocks private MemberOnboardingService onboardingService;

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
                "refresh-token"
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
    @DisplayName("유효한 임시 데이터로 가입 성공") // "signup succeeds with valid temporary data"
    void shouldSignupWithValidTemporaryData() {
        given(redisMemberDataAdapter.getTempData("uuid-123")).willReturn(Optional.of(socialProfile));
        given(globalSocialTokenCommandAdapter.save(any(SocialToken.class)))
                .willReturn(SocialToken.createSocialToken("access-token", "refresh-token"));
        given(memberRepository.save(any(Member.class))).willReturn(persistedMember);
        given(globalAuthTokenSaveAdapter.save(any(AuthToken.class))).willReturn(persistedAuthToken);
        given(globalJwtAdapter.generateAccessToken(any(CustomUserDetails.class))).willReturn("access-jwt");
        given(globalJwtAdapter.generateRefreshToken(any(CustomUserDetails.class))).willReturn("refresh-jwt");
        given(globalCookieAdapter.generateJwtCookie("access-jwt", "refresh-jwt")).willReturn(jwtCookies);

        List<ResponseCookie> result = onboardingService.signup("tester", "uuid-123");

        assertThat(result).isEqualTo(jwtCookies);
        verify(redisMemberDataAdapter).getTempData("uuid-123");
        verify(globalSocialTokenCommandAdapter).save(any(SocialToken.class));
        verify(memberRepository).save(any(Member.class));
        verify(globalAuthTokenSaveAdapter).save(any(AuthToken.class));

        ArgumentCaptor<CustomUserDetails> detailCaptor = ArgumentCaptor.forClass(CustomUserDetails.class);
        verify(globalJwtAdapter).generateAccessToken(detailCaptor.capture());
        verify(globalJwtAdapter).generateRefreshToken(detailCaptor.getValue());

        verify(globalAuthTokenSaveAdapter).updateJwtRefreshToken(persistedAuthToken.getId(), "refresh-jwt");
        verify(globalCookieAdapter).generateJwtCookie("access-jwt", "refresh-jwt");
        verify(redisMemberDataAdapter).removeTempData("uuid-123");

        CustomUserDetails capturedDetail = detailCaptor.getValue();
        assertThat(capturedDetail.getAuthTokenId()).isEqualTo(persistedAuthToken.getId());
    }

    @Test
    @DisplayName("임시 데이터가 없을 때 가입 실패") // "signup fails when temp data is missing"
    void shouldThrowExceptionWhenTempDataMissing() {
        given(redisMemberDataAdapter.getTempData("missing-uuid")).willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.signup("tester", "missing-uuid"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TEMP_DATA);

        verify(redisMemberDataAdapter).getTempData("missing-uuid");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("가입 시 JWT 토큰 및 쿠키 발급") // "signup issues jwt tokens and cookies"
    void shouldGenerateJwtTokensAndCookiesOnSignup() {
        given(redisMemberDataAdapter.getTempData("uuid-123")).willReturn(Optional.of(socialProfile));
        given(globalSocialTokenCommandAdapter.save(any(SocialToken.class)))
                .willReturn(SocialToken.createSocialToken("access-token", "refresh-token"));
        given(memberRepository.save(any(Member.class))).willReturn(persistedMember);
        given(globalAuthTokenSaveAdapter.save(any(AuthToken.class))).willReturn(persistedAuthToken);
        given(globalJwtAdapter.generateAccessToken(any(CustomUserDetails.class))).willReturn("access-jwt");
        given(globalJwtAdapter.generateRefreshToken(any(CustomUserDetails.class))).willReturn("refresh-jwt");
        given(globalCookieAdapter.generateJwtCookie("access-jwt", "refresh-jwt")).willReturn(jwtCookies);

        List<ResponseCookie> result = onboardingService.signup("tester", "uuid-123");

        assertThat(result).isEqualTo(jwtCookies);

        ArgumentCaptor<CustomUserDetails> detailCaptor = ArgumentCaptor.forClass(CustomUserDetails.class);
        verify(globalJwtAdapter).generateAccessToken(detailCaptor.capture());
        verify(globalJwtAdapter).generateRefreshToken(detailCaptor.getValue());

        CustomUserDetails capturedDetail = detailCaptor.getValue();
        assertThat(capturedDetail.getAuthTokenId()).isEqualTo(persistedAuthToken.getId());
    }
}