package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.adapter.AuthToJwtAdapter;
import jaeik.bimillog.domain.auth.adapter.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.repository.AuthTokenRepository;
import jaeik.bimillog.domain.auth.repository.BlackListRepository;
import jaeik.bimillog.domain.auth.repository.SocialTokenRepository;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
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
    @Mock private AuthToJwtAdapter authToJwtAdapter;
    @Mock private SocialTokenRepository socialTokenRepository;
    @Mock private AuthTokenRepository authTokenRepository;

    @InjectMocks
    private SocialLoginTransactionalService socialLoginTransactionalService;

    @Test
    @DisplayName("기존 회원 로그인 처리 성공 - 소셜 토큰이 있는 경우")
    void shouldFinishLoginForExistingMemberWithSocialToken() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        Member existingMember = TestMembers.createMember(TEST_SOCIAL_ID, "memberName", TEST_SOCIAL_NICKNAME);
        TestFixtures.setFieldValue(existingMember, "id", 1L);
        if (existingMember.getSetting() != null) {
            TestFixtures.setFieldValue(existingMember.getSetting(), "id", 10L);
        }

        // 기존 소셜 토큰 설정
        SocialToken existingSocialToken = SocialToken.createSocialToken("old-access-token", "old-refresh-token");
        TestFixtures.setFieldValue(existingMember, "socialToken", existingSocialToken);

        CustomUserDetails userDetails = CustomUserDetails.ofExisting(existingMember, 99L);

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.of(existingMember));
        given(authToMemberAdapter.handleExistingMember(eq(existingMember), anyString(), anyString(), eq(existingSocialToken)))
                .willReturn(existingMember);

        AuthToken persistedAuthToken = AuthToken.builder()
                .id(userDetails.getAuthTokenId())
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();
        given(authTokenRepository.save(any(AuthToken.class))).willReturn(persistedAuthToken);

        given(authToJwtAdapter.generateAccessToken(any(CustomUserDetails.class))).willReturn("generated-access-token");
        given(authToJwtAdapter.generateRefreshToken(any(CustomUserDetails.class))).willReturn("generated-refresh-token");

        // When
        LoginResult result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
        LoginResult.ExistingUser existingUser = (LoginResult.ExistingUser) result;
        assertThat(existingUser.tokens().accessToken()).isEqualTo("generated-access-token");
        assertThat(existingUser.tokens().refreshToken()).isEqualTo("generated-refresh-token");

        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(authToMemberAdapter).findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(authToMemberAdapter).handleExistingMember(eq(existingMember), eq(profile.getNickname()), eq(profile.getProfileImageUrl()), eq(existingSocialToken));
    }

    @Test
    @DisplayName("기존 회원 로그인 처리 성공 - 소셜 토큰이 없는 경우 (이전 버전에서 로그아웃)")
    void shouldFinishLoginForExistingMemberWithoutSocialToken() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        Member existingMember = TestMembers.createMember(TEST_SOCIAL_ID, "memberName", TEST_SOCIAL_NICKNAME);
        TestFixtures.setFieldValue(existingMember, "id", 1L);
        if (existingMember.getSetting() != null) {
            TestFixtures.setFieldValue(existingMember.getSetting(), "id", 10L);
        }

        // 소셜 토큰이 null인 상태 (이전 버전에서 로그아웃으로 삭제된 경우)
        TestFixtures.setFieldValue(existingMember, "socialToken", null);

        SocialToken newSocialToken = SocialToken.createSocialToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        CustomUserDetails userDetails = CustomUserDetails.ofExisting(existingMember, 99L);

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.of(existingMember));
        given(socialTokenRepository.save(any(SocialToken.class))).willReturn(newSocialToken);
        given(authToMemberAdapter.handleExistingMember(eq(existingMember), anyString(), anyString(), eq(newSocialToken)))
                .willReturn(existingMember);

        AuthToken persistedAuthToken = AuthToken.builder()
                .id(userDetails.getAuthTokenId())
                .refreshToken("")
                .member(existingMember)
                .useCount(0)
                .build();
        given(authTokenRepository.save(any(AuthToken.class))).willReturn(persistedAuthToken);

        given(authToJwtAdapter.generateAccessToken(any(CustomUserDetails.class))).willReturn("generated-access-token");
        given(authToJwtAdapter.generateRefreshToken(any(CustomUserDetails.class))).willReturn("generated-refresh-token");

        // When
        LoginResult result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
        LoginResult.ExistingUser existingUser = (LoginResult.ExistingUser) result;
        assertThat(existingUser.tokens().accessToken()).isEqualTo("generated-access-token");
        assertThat(existingUser.tokens().refreshToken()).isEqualTo("generated-refresh-token");

        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(authToMemberAdapter).findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(socialTokenRepository).save(any(SocialToken.class));
        verify(authToMemberAdapter).handleExistingMember(eq(existingMember), eq(profile.getNickname()), eq(profile.getProfileImageUrl()), eq(newSocialToken));
    }

    @Test
    @DisplayName("신규 회원 로그인 처리 성공")
    void shouldFinishLoginForNewMember() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.empty());

        // When
        LoginResult result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result).isInstanceOf(LoginResult.NewUser.class);
        LoginResult.NewUser newUser = (LoginResult.NewUser) result;
        assertThat(newUser.tempUserId()).isNotBlank();

        verify(authToMemberAdapter).handleNewUser(eq(profile), eq(newUser.tempUserId()));
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
