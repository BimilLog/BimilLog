package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.application.service.MemberSaveService;
import jaeik.bimillog.domain.auth.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>MemberSaveService 단위 테스트</h2>
 * <p>소셜 로그인 시 사용자 데이터 저장 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>기존 사용자와 신규 사용자 처리 로직을 중심으로 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("MemberSaveService 단위 테스트")
@Tag("unit")
class MemberSaveServiceTest extends BaseUnitTest {

    @Mock
    private MemberQueryPort memberQueryPort;

    @Mock
    private SaveMemberPort saveMemberPort;

    @Mock
    private RedisMemberDataPort redisMemberDataPort;

    @InjectMocks
    private MemberSaveService userSaveService;

    private SocialMemberProfile testSocialProfile;
    private String testFcmToken;

    @BeforeEach
    protected void setUpChild() {
        testFcmToken = "fcm-TemporaryToken-123";
        testSocialProfile = new SocialMemberProfile(
            "kakao123",
            "test@example.com",
            SocialProvider.KAKAO,
            "testNickname",
            "profile.jpg",
            "access-TemporaryToken",
            "refresh-TemporaryToken",
            testFcmToken
        );
    }

    @Test
    @DisplayName("기존 사용자 처리 - MemberDetail 반환")
    void shouldReturnExistingUserDetail_WhenExistingUser() {
        // Given
        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.of(getTestMember()));

        MemberDetail expectedDetail = MemberDetail.ofExisting(getTestMember(), 1L, 100L);
        given(saveMemberPort.handleExistingUserData(
            getTestMember(),
            testSocialProfile
        )).willReturn(expectedDetail);

        // When
        MemberDetail result = userSaveService.processUserData(testSocialProfile);

        // Then
        assertThat(result).isInstanceOf(MemberDetail.class);
        MemberDetail memberDetail = (MemberDetail) result;
        assertThat(memberDetail.getMemberId()).isEqualTo(getTestMember().getId());
        assertThat(memberDetail.getTokenId()).isEqualTo(1L);
        assertThat(memberDetail.getFcmTokenId()).isEqualTo(100L);

        verify(memberQueryPort).findByProviderAndSocialId(SocialProvider.KAKAO, "kakao123");
        verify(saveMemberPort).handleExistingUserData(getTestMember(), testSocialProfile);
        verify(redisMemberDataPort, never()).saveTempData(any(), any());
    }

    @Test
    @DisplayName("신규 사용자 처리 - NewMemberDetail 반환")
    void shouldReturnNewUserDetail_WhenNewUser() {
        // Given
        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.empty());

        // When
        MemberDetail result = userSaveService.processUserData(testSocialProfile);

        // Then
        assertThat(result.getUuid()).isNotNull();
        assertThat(result.getUuid()).isNotEmpty();

        // Redis에 임시 데이터 저장 검증
        ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SocialMemberProfile> profileCaptor = ArgumentCaptor.forClass(SocialMemberProfile.class);

        verify(redisMemberDataPort).saveTempData(
            uuidCaptor.capture(),
            profileCaptor.capture()
        );

        assertThat(uuidCaptor.getValue()).isEqualTo(result.getUuid());
        assertThat(profileCaptor.getValue()).isEqualTo(testSocialProfile);

        verify(saveMemberPort, never()).handleExistingUserData(any(), any());
    }

    @Test
    @DisplayName("FCM 토큰이 null인 경우 처리")
    void shouldProcessUserData_WhenFcmTokenIsNull() {
        // Given
        SocialMemberProfile profileWithoutFcm = new SocialMemberProfile(
            "kakao123",
            "test@example.com",
            SocialProvider.KAKAO,
            "testNickname",
            "profile.jpg",
            "access-TemporaryToken",
            "refresh-TemporaryToken",
            null
        );

        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.of(getTestMember()));

        MemberDetail expectedDetail = MemberDetail.ofExisting(getTestMember(), 1L, null);
        given(saveMemberPort.handleExistingUserData(
            getTestMember(),
            profileWithoutFcm
        )).willReturn(expectedDetail);

        // When
        MemberDetail result = userSaveService.processUserData(profileWithoutFcm);

        // Then
        assertThat(result).isInstanceOf(MemberDetail.class);
        MemberDetail memberDetail = (MemberDetail) result;
        assertThat(memberDetail.getFcmTokenId()).isNull();

        verify(saveMemberPort).handleExistingUserData(getTestMember(), profileWithoutFcm);
    }

    @Test
    @DisplayName("신규 사용자 - FCM 토큰 없이 처리")
    void shouldHandleNewMember_WithoutFcmToken() {
        // Given
        SocialMemberProfile profileWithoutFcm = new SocialMemberProfile(
            "kakao123",
            "test@example.com",
            SocialProvider.KAKAO,
            "testNickname",
            "profile.jpg",
            "access-TemporaryToken",
            "refresh-TemporaryToken",
            null
        );

        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.empty());

        // When
        MemberDetail result = userSaveService.processUserData(profileWithoutFcm);

        // Then
        assertThat(result.getUuid()).isNotNull();

        // Redis에 FCM 토큰 없이 저장되는지 검증
        verify(redisMemberDataPort).saveTempData(
            eq(result.getUuid()),
            eq(profileWithoutFcm)
        );
    }

    @Test
    @DisplayName("다른 소셜 프로바이더로 기존 사용자 조회")
    void shouldFindExistingUser_WithDifferentProvider() {
        // Given
        SocialMemberProfile googleProfile = new SocialMemberProfile(
            "google456",
            "google@example.com",
            SocialProvider.GOOGLE,
            "googleMember",
            "google-profile.jpg",
            "access-token",
            "refresh-token",
            testFcmToken
        );

        Member googleMember = getOtherMember();
        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.GOOGLE,
            "google456"
        )).willReturn(Optional.of(googleMember));

        MemberDetail expectedDetail = MemberDetail.ofExisting(googleMember, 2L, 200L);
        given(saveMemberPort.handleExistingUserData(
                googleMember,
            googleProfile
        )).willReturn(expectedDetail);

        // When
        MemberDetail result = userSaveService.processUserData(googleProfile);

        // Then
        assertThat(result).isInstanceOf(MemberDetail.class);
        verify(memberQueryPort).findByProviderAndSocialId(SocialProvider.GOOGLE, "google456");
        verify(saveMemberPort).handleExistingUserData(googleMember, googleProfile);
    }
}