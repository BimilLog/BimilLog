package jaeik.bimillog.unit.domain.auth;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.dto.LoginResultDTO;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.adapter.AuthToJwtAdapter;
import jaeik.bimillog.domain.auth.adapter.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.repository.AuthTokenRepository;
import jaeik.bimillog.domain.auth.repository.BlackListRepository;
import jaeik.bimillog.domain.auth.repository.SocialTokenRepository;
import jaeik.bimillog.domain.auth.service.SocialLoginTransactionalService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
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

    // ==================== 공통 헬퍼 ====================

    private SocialMemberProfile getTestMemberProfile() {
        return SocialMemberProfile.of(
                TEST_SOCIAL_ID, TEST_EMAIL, TEST_PROVIDER,
                TEST_SOCIAL_NICKNAME, TEST_PROFILE_IMAGE,
                TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN
        );
    }

    private AuthToken stubAuthToken(Member member) {
        AuthToken token = AuthToken.builder()
                .id(99L)
                .refreshToken("")
                .member(member)
                .useCount(0)
                .build();
        given(authTokenRepository.save(any(AuthToken.class))).willReturn(token);
        return token;
    }

    private void stubJwtAdapters() {
        given(authToJwtAdapter.generateAccessToken(any(CustomUserDetails.class))).willReturn("generated-access-token");
        given(authToJwtAdapter.generateRefreshToken(any(CustomUserDetails.class))).willReturn("generated-refresh-token");
    }

    // ==================== 기존 회원 ====================

    @Test
    @DisplayName("기존 회원 로그인 - 소셜 토큰이 있는 경우 JWT 발급 성공")
    void shouldFinishLoginForExistingMemberWithSocialToken() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        Member existingMember = TestMembers.createMember(TEST_SOCIAL_ID, "memberName", TEST_SOCIAL_NICKNAME);
        TestFixtures.setFieldValue(existingMember, "id", 1L);
        if (existingMember.getSetting() != null) {
            TestFixtures.setFieldValue(existingMember.getSetting(), "id", 10L);
        }

        SocialToken existingSocialToken = SocialToken.createSocialToken("old-access-token", "old-refresh-token");
        TestFixtures.setFieldValue(existingMember, "socialToken", existingSocialToken);

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.of(existingMember));
        given(authToMemberAdapter.handleExistingMember(eq(existingMember), anyString(), anyString(), eq(existingSocialToken)))
                .willReturn(existingMember);
        stubAuthToken(existingMember);
        stubJwtAdapters();

        // When
        LoginResultDTO result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result.getJwtAccessToken()).isEqualTo("generated-access-token");
        assertThat(result.getJwtRefreshToken()).isEqualTo("generated-refresh-token");

        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(authToMemberAdapter).findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        verify(authToMemberAdapter).handleExistingMember(
                eq(existingMember), eq(profile.getNickname()), eq(profile.getProfileImageUrl()), eq(existingSocialToken));
    }

    @Test
    @DisplayName("기존 회원 로그인 - 소셜 토큰이 없는 경우(이전 버전 로그아웃) 신규 토큰 생성 후 JWT 발급")
    void shouldFinishLoginForExistingMemberWithoutSocialToken() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        Member existingMember = TestMembers.createMember(TEST_SOCIAL_ID, "memberName", TEST_SOCIAL_NICKNAME);
        TestFixtures.setFieldValue(existingMember, "id", 1L);
        if (existingMember.getSetting() != null) {
            TestFixtures.setFieldValue(existingMember.getSetting(), "id", 10L);
        }
        TestFixtures.setFieldValue(existingMember, "socialToken", null);

        SocialToken newSocialToken = SocialToken.createSocialToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.of(existingMember));
        given(socialTokenRepository.save(any(SocialToken.class))).willReturn(newSocialToken);
        given(authToMemberAdapter.handleExistingMember(eq(existingMember), anyString(), anyString(), eq(newSocialToken)))
                .willReturn(existingMember);
        stubAuthToken(existingMember);
        stubJwtAdapters();

        // When
        LoginResultDTO result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result.getJwtAccessToken()).isEqualTo("generated-access-token");
        assertThat(result.getJwtRefreshToken()).isEqualTo("generated-refresh-token");

        verify(socialTokenRepository).save(any(SocialToken.class));
        verify(authToMemberAdapter).handleExistingMember(
                eq(existingMember), eq(profile.getNickname()), eq(profile.getProfileImageUrl()), eq(newSocialToken));
    }

    // ==================== 신규 회원 ====================

    @Test
    @DisplayName("신규 회원 로그인 - 즉시 가입 후 JWT 발급 성공")
    void shouldFinishLoginForNewMember() {
        // Given
        SocialMemberProfile profile = getTestMemberProfile();
        SocialToken persistedSocialToken = SocialToken.createSocialToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        Member newMember = TestMembers.createMember(TEST_SOCIAL_ID, "냥_a3f8c2", TEST_SOCIAL_NICKNAME);
        TestFixtures.setFieldValue(newMember, "id", 2L);

        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
        given(authToMemberAdapter.findByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(Optional.empty());
        given(socialTokenRepository.save(any(SocialToken.class))).willReturn(persistedSocialToken);
        given(authToMemberAdapter.handleNewMember(any(SocialMemberProfile.class), eq(persistedSocialToken))).willReturn(newMember);
        stubAuthToken(newMember);
        stubJwtAdapters();

        // When
        LoginResultDTO result = socialLoginTransactionalService.finishLogin(TEST_PROVIDER, profile);

        // Then
        assertThat(result.getJwtAccessToken()).isEqualTo("generated-access-token");
        assertThat(result.getJwtRefreshToken()).isEqualTo("generated-refresh-token");

        verify(socialTokenRepository).save(any(SocialToken.class));
        verify(authToMemberAdapter).handleNewMember(any(SocialMemberProfile.class), eq(persistedSocialToken));
        verify(authTokenRepository).save(any(AuthToken.class));
    }

    // ==================== 블랙리스트 ====================

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
}
