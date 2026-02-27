package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.repository.MemberRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>MemberOnboardingService 단위 테스트</h2>
 * <p>신규/기존 회원 온보딩 흐름을 검증합니다.</p>
 */
@DisplayName("MemberOnboardingService 단위 테스트")
@Tag("unit")
class MemberOnboardingServiceTest extends BaseUnitTest {

    @Mock private MemberRepository memberRepository;

    @InjectMocks private MemberOnboardingService onboardingService;

    private SocialMemberProfile socialProfile;
    private SocialToken socialToken;
    private Member persistedMember;

    @BeforeEach
    void setUp() {
        socialProfile = SocialMemberProfile.of(
                "kakao123",
                "signup@example.com",
                SocialProvider.KAKAO,
                "signupNickname",
                "profile.jpg",
                "access-token",
                "refresh-token"
        );

        socialToken = SocialToken.createSocialToken("access-token", "refresh-token");

        persistedMember = TestMembers.createMember("kakao123", "냥_a3f8c2", "signupNickname");
        TestFixtures.setFieldValue(persistedMember, "id", 1L);
        if (persistedMember.getSetting() != null) {
            TestFixtures.setFieldValue(persistedMember.getSetting(), "id", 10L);
        }
    }

    // ==================== signup ====================

    @Test
    @DisplayName("signup - 이름 충돌 없으면 첫 시도에 가입 성공")
    void shouldSignupSuccessOnFirstAttempt() {
        // Given
        given(memberRepository.existsByMemberName(anyString())).willReturn(false);
        given(memberRepository.save(any(Member.class))).willReturn(persistedMember);

        // When
        Member result = onboardingService.signup(socialProfile, socialToken);

        // Then
        assertThat(result).isNotNull();
        verify(memberRepository, times(1)).existsByMemberName(anyString());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("signup - 이름 충돌 시 재시도하여 가입 성공")
    void shouldRetryWhenTempNameCollides() {
        // Given: 첫 번째는 충돌, 두 번째는 통과
        given(memberRepository.existsByMemberName(anyString()))
                .willReturn(true)
                .willReturn(false);
        given(memberRepository.save(any(Member.class))).willReturn(persistedMember);

        // When
        Member result = onboardingService.signup(socialProfile, socialToken);

        // Then
        assertThat(result).isNotNull();
        verify(memberRepository, times(2)).existsByMemberName(anyString());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("signup - 생성된 임시 이름은 '냥_' 접두어로 시작하고 총 8자이다")
    void shouldGenerateTempNameWithCorrectFormat() {
        // Given
        given(memberRepository.existsByMemberName(anyString())).willReturn(false);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        given(memberRepository.save(memberCaptor.capture())).willReturn(persistedMember);

        // When
        onboardingService.signup(socialProfile, socialToken);

        // Then
        String generatedName = memberCaptor.getValue().getMemberName();
        assertThat(generatedName).startsWith("냥_");
        assertThat(generatedName).hasSize(8);
    }

    @Test
    @DisplayName("signup - 소셜 프로필 정보가 Member에 올바르게 반영된다")
    void shouldMapSocialProfileToMember() {
        // Given
        given(memberRepository.existsByMemberName(anyString())).willReturn(false);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        given(memberRepository.save(memberCaptor.capture())).willReturn(persistedMember);

        // When
        onboardingService.signup(socialProfile, socialToken);

        // Then
        Member saved = memberCaptor.getValue();
        assertThat(saved.getSocialId()).isEqualTo(socialProfile.getSocialId());
        assertThat(saved.getProvider()).isEqualTo(socialProfile.getProvider());
        assertThat(saved.getSocialNickname()).isEqualTo(socialProfile.getNickname());
        assertThat(saved.getThumbnailImage()).isEqualTo(socialProfile.getProfileImageUrl());
        assertThat(saved.getSocialToken()).isEqualTo(socialToken);
    }

    // ==================== syncExistingMember ====================

    @Test
    @DisplayName("syncExistingMember - 닉네임·프로필·소셜토큰이 업데이트된다")
    void shouldSyncExistingMember() {
        // Given
        Member member = TestMembers.createMember("kakao-1", "tester", "oldNickname");
        TestFixtures.setFieldValue(member, "id", 1L);
        SocialToken newToken = SocialToken.createSocialToken("new-access", "new-refresh");

        // When
        Member result = onboardingService.syncExistingMember(member, "newNickname", "http://img/new.jpg", newToken);

        // Then
        assertThat(result).isSameAs(member);
        assertThat(member.getSocialNickname()).isEqualTo("newNickname");
        assertThat(member.getThumbnailImage()).isEqualTo("http://img/new.jpg");
        assertThat(member.getSocialToken()).isEqualTo(newToken);

        verifyNoInteractions(memberRepository);
    }
}
