package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.out.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.global.out.GlobalBlacklistAdapter;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalKakaoTokenCommandAdapter;
import jaeik.bimillog.domain.global.out.GlobalLoginAdapter;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>SocialLoginTransactionalService 단위 테스트</h2>
 * <p>DB/토큰 갱신 트랜잭션 흐름 검증</p>
 */
@DisplayName("SocialLoginTransactionalService 단위 테스트")
@Tag("unit")
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialLoginTransactionalServiceTest extends BaseUnitTest {

    @Mock private AuthToMemberAdapter authToMemberAdapter;
    @Mock private GlobalLoginAdapter globalLoginAdapter;
    @Mock private GlobalBlacklistAdapter globalBlacklistAdapter;
    @Mock private GlobalCookieAdapter globalCookieAdapter;
    @Mock private GlobalJwtAdapter globalJwtAdapter;
    @Mock private GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;
    @Mock private GlobalKakaoTokenCommandAdapter globalKakaoTokenCommandAdapter;

    @InjectMocks
    private SocialLoginTransactionalService socialLoginTransactionalService;

    @Test
    @DisplayName("기존 회원 로그인 처리 성공")
    void shouldFinishLoginForExistingMember() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        Member existingMember = TestMembers.createMember(TEST_SOCIAL_ID, "memberName", TEST_SOCIAL_NICKNAME);
        TestFixtures.setFieldValue(existingMember, "id", 1L);
        if (existingMember.getSetting() != null) {
            TestFixtures.setFieldValue(existingMember.getSetting(), "id", 10L);
        }

        MemberDetail memberDetail = MemberDetail.ofExisting(existingMember, 99L);
        List<ResponseCookie> jwtCookies = getJwtCookies();

        given(globalBlacklistAdapter.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(globalLoginAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.of(existingMember));

        KakaoToken persistedKakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        given(globalKakaoTokenCommandAdapter.save(any(KakaoToken.class))).willReturn(persistedKakaoToken);
        given(authToMemberAdapter.handleExistingMember(eq(existingMember), anyString(), anyString(), eq(persistedKakaoToken)))
                .willReturn(existingMember);

        AuthToken persistedAuthToken = AuthToken.builder()
                .id(memberDetail.getAuthTokenId())
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();
        given(globalAuthTokenSaveAdapter.save(any(AuthToken.class))).willReturn(persistedAuthToken);

        given(globalJwtAdapter.generateAccessToken(any(MemberDetail.class))).willReturn("generated-access-token");
        given(globalJwtAdapter.generateRefreshToken(any(MemberDetail.class))).willReturn("generated-refresh-token");
        given(globalCookieAdapter.generateJwtCookie(anyString(), anyString())).willReturn(jwtCookies);

        // When
        LoginResult result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
        LoginResult.ExistingUser existingUser = (LoginResult.ExistingUser) result;
        assertThat(existingUser.cookies()).isEqualTo(jwtCookies);

        verify(globalBlacklistAdapter).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(globalLoginAdapter).findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(globalKakaoTokenCommandAdapter).save(any(KakaoToken.class));
        verify(authToMemberAdapter).handleExistingMember(eq(existingMember), eq(profile.getNickname()), eq(profile.getProfileImageUrl()), eq(persistedKakaoToken));
        verify(globalAuthTokenSaveAdapter).updateJwtRefreshToken(memberDetail.getAuthTokenId(), "generated-refresh-token");
        verify(globalCookieAdapter).generateJwtCookie("generated-access-token", "generated-refresh-token");
    }

    @Test
    @DisplayName("신규 회원 로그인 처리 성공")
    void shouldFinishLoginForNewMember() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();

        given(globalBlacklistAdapter.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(globalLoginAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.empty());
        given(globalCookieAdapter.createTempCookie(anyString())).willAnswer(invocation -> {
            String generatedUuid = invocation.getArgument(0);
            return ResponseCookie.from("temp", generatedUuid)
                    .path("/")
                    .maxAge(Duration.ofMinutes(10))
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .build();
        });

        // When
        LoginResult result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result).isInstanceOf(LoginResult.NewUser.class);
        LoginResult.NewUser newUser = (LoginResult.NewUser) result;
        assertThat(newUser.tempCookie()).isNotNull();
        assertThat(newUser.tempCookie().getValue()).isNotBlank();

        verify(authToMemberAdapter).handleNewUser(eq(profile), eq(newUser.tempCookie().getValue()));
        verify(globalCookieAdapter).createTempCookie(newUser.tempCookie().getValue());
    }

    @Test
    @DisplayName("블랙리스트 사용자는 예외 발생")
    void shouldThrowExceptionForBlacklistedUser() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        given(globalBlacklistAdapter.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.BLACKLIST_USER);
    }

    private List<ResponseCookie> getJwtCookies() {
        return Arrays.asList(
                ResponseCookie.from("accessToken", "test-access-token")
                        .maxAge(3600)
                        .path("/")
                        .secure(true)
                        .httpOnly(true)
                        .sameSite("Strict")
                        .build(),
                ResponseCookie.from("refreshToken", "test-refresh-token")
                        .maxAge(86400)
                        .path("/")
                        .secure(true)
                        .httpOnly(true)
                        .sameSite("Strict")
                        .build()
        );
    }

    private SocialMemberProfile getTestMemberProfile() {
        return SocialMemberProfile.of(
                AuthTestFixtures.TEST_SOCIAL_ID,
                AuthTestFixtures.TEST_EMAIL,
                AuthTestFixtures.TEST_PROVIDER,
                AuthTestFixtures.TEST_SOCIAL_NICKNAME,
                AuthTestFixtures.TEST_PROFILE_IMAGE,
                AuthTestFixtures.TEST_ACCESS_TOKEN,
                AuthTestFixtures.TEST_REFRESH_TOKEN
        );
    }
}
