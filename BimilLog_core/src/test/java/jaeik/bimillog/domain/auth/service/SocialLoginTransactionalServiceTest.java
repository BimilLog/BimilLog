package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.out.AuthTokenRepository;
import jaeik.bimillog.domain.auth.out.BlackListRepository;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
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
    @Mock private BlackListRepository blackListRepository;
    @Mock private GlobalCookieAdapter globalCookieAdapter;
    @Mock private GlobalJwtAdapter globalJwtAdapter;
    @Mock private GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;
    @Mock private GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;
    @Mock private AuthTokenRepository authTokenRepository;

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

        CustomUserDetails userDetails = CustomUserDetails.ofExisting(existingMember, 99L);
        List<ResponseCookie> jwtCookies = getJwtCookies();

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.of(existingMember));

        SocialToken persistedSocialToken = SocialToken.createSocialToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        given(globalSocialTokenCommandAdapter.save(any(SocialToken.class))).willReturn(persistedSocialToken);
        given(authToMemberAdapter.handleExistingMember(eq(existingMember), anyString(), anyString(), eq(persistedSocialToken)))
                .willReturn(existingMember);

        AuthToken persistedAuthToken = AuthToken.builder()
                .id(userDetails.getAuthTokenId())
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();
        given(authTokenRepository.save(any(AuthToken.class))).willReturn(persistedAuthToken);

        given(globalJwtAdapter.generateAccessToken(any(CustomUserDetails.class))).willReturn("generated-access-token");
        given(globalJwtAdapter.generateRefreshToken(any(CustomUserDetails.class))).willReturn("generated-refresh-token");
        given(globalCookieAdapter.generateJwtCookie(anyString(), anyString())).willReturn(jwtCookies);

        // When
        LoginResult result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
        LoginResult.ExistingUser existingUser = (LoginResult.ExistingUser) result;
        assertThat(existingUser.cookies()).isEqualTo(jwtCookies);

        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(authToMemberAdapter).findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(globalSocialTokenCommandAdapter).save(any(SocialToken.class));
        verify(authToMemberAdapter).handleExistingMember(eq(existingMember), eq(profile.getNickname()), eq(profile.getProfileImageUrl()), eq(persistedSocialToken));
        verify(globalAuthTokenSaveAdapter).updateJwtRefreshToken(userDetails.getAuthTokenId(), "generated-refresh-token");
        verify(globalCookieAdapter).generateJwtCookie("generated-access-token", "generated-refresh-token");
    }

    @Test
    @DisplayName("신규 회원 로그인 처리 성공")
    void shouldFinishLoginForNewMember() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.empty());
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
        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_BLACKLIST_USER);
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
