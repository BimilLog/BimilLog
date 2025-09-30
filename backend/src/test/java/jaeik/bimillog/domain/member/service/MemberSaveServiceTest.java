package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.application.service.MemberSaveService;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.entity.memberdetail.ExistingMemberDetail;
import jaeik.bimillog.domain.member.entity.memberdetail.NewMemberDetail;
import jaeik.bimillog.domain.member.entity.memberdetail.MemberDetail;
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
@Tag("test")
class MemberSaveServiceTest extends BaseUnitTest {

    @Mock
    private MemberQueryPort memberQueryPort;

    @Mock
    private SaveMemberPort saveMemberPort;

    @Mock
    private RedisMemberDataPort redisMemberDataPort;

    @InjectMocks
    private MemberSaveService userSaveService;

    private SocialUserProfile testSocialProfile;
    private String testFcmToken;

    @BeforeEach
    protected void setUpChild() {
        testFcmToken = "fcm-TemporaryToken-123";
        testSocialProfile = new SocialUserProfile(
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
    @DisplayName("기존 사용자 처리 - ExistingMemberDetail 반환")
    void shouldReturnExistingUserDetail_WhenExistingUser() {
        // Given
        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.KAKAO,
            "kakao123"
        )).willReturn(Optional.of(getTestUser()));

        ExistingMemberDetail expectedDetail = ExistingMemberDetail.of(getTestUser(), 1L, 100L);
        given(saveMemberPort.handleExistingUserData(
            getTestUser(),
            testSocialProfile
        )).willReturn(expectedDetail);

        // When
        MemberDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile
        );

        // Then
        assertThat(result).isInstanceOf(ExistingMemberDetail.class);
        ExistingMemberDetail existingMemberDetail = (ExistingMemberDetail) result;
        assertThat(existingMemberDetail.getUserId()).isEqualTo(getTestUser().getId());
        assertThat(existingMemberDetail.getTokenId()).isEqualTo(1L);
        assertThat(existingMemberDetail.getFcmTokenId()).isEqualTo(100L);

        verify(memberQueryPort).findByProviderAndSocialId(SocialProvider.KAKAO, "kakao123");
        verify(saveMemberPort).handleExistingUserData(getTestUser(), testSocialProfile);
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
        MemberDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            testSocialProfile
        );

        // Then
        assertThat(result).isInstanceOf(NewMemberDetail.class);
        NewMemberDetail newMemberDetail = (NewMemberDetail) result;
        assertThat(newMemberDetail.getUuid()).isNotNull();
        assertThat(newMemberDetail.getUuid()).isNotEmpty();

        // Redis에 임시 데이터 저장 검증
        ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SocialUserProfile> profileCaptor = ArgumentCaptor.forClass(SocialUserProfile.class);

        verify(redisMemberDataPort).saveTempData(
            uuidCaptor.capture(),
            profileCaptor.capture()
        );

        assertThat(uuidCaptor.getValue()).isEqualTo(newMemberDetail.getUuid());
        assertThat(profileCaptor.getValue()).isEqualTo(testSocialProfile);

        verify(saveMemberPort, never()).handleExistingUserData(any(), any());
    }

    @Test
    @DisplayName("FCM 토큰이 null인 경우 처리")
    void shouldProcessUserData_WhenFcmTokenIsNull() {
        // Given
        SocialUserProfile profileWithoutFcm = new SocialUserProfile(
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
        )).willReturn(Optional.of(getTestUser()));

        ExistingMemberDetail expectedDetail = ExistingMemberDetail.of(getTestUser(), 1L, null);
        given(saveMemberPort.handleExistingUserData(
            getTestUser(),
            profileWithoutFcm
        )).willReturn(expectedDetail);

        // When
        MemberDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            profileWithoutFcm
        );

        // Then
        assertThat(result).isInstanceOf(ExistingMemberDetail.class);
        ExistingMemberDetail existingMemberDetail = (ExistingMemberDetail) result;
        assertThat(existingMemberDetail.getFcmTokenId()).isNull();

        verify(saveMemberPort).handleExistingUserData(getTestUser(), profileWithoutFcm);
    }

    @Test
    @DisplayName("신규 사용자 - FCM 토큰 없이 처리")
    void shouldHandleNewUser_WithoutFcmToken() {
        // Given
        SocialUserProfile profileWithoutFcm = new SocialUserProfile(
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
        MemberDetail result = userSaveService.processUserData(
            SocialProvider.KAKAO,
            profileWithoutFcm
        );

        // Then
        assertThat(result).isInstanceOf(NewMemberDetail.class);
        NewMemberDetail newMemberDetail = (NewMemberDetail) result;

        // Redis에 FCM 토큰 없이 저장되는지 검증
        verify(redisMemberDataPort).saveTempData(
            eq(newMemberDetail.getUuid()),
            eq(profileWithoutFcm)
        );
    }

    @Test
    @DisplayName("다른 소셜 프로바이더로 기존 사용자 조회")
    void shouldFindExistingUser_WithDifferentProvider() {
        // Given
        SocialUserProfile googleProfile = new SocialUserProfile(
            "google456",
            "google@example.com",
            SocialProvider.GOOGLE,
            "googleMember",
            "google-profile.jpg",
            "access-token",
            "refresh-token",
            testFcmToken
        );

        Member googleMember = getOtherUser();
        given(memberQueryPort.findByProviderAndSocialId(
            SocialProvider.GOOGLE,
            "google456"
        )).willReturn(Optional.of(googleMember));

        ExistingMemberDetail expectedDetail = ExistingMemberDetail.of(googleMember, 2L, 200L);
        given(saveMemberPort.handleExistingUserData(
                googleMember,
            googleProfile
        )).willReturn(expectedDetail);

        // When
        MemberDetail result = userSaveService.processUserData(
            SocialProvider.GOOGLE,
            googleProfile
        );

        // Then
        assertThat(result).isInstanceOf(ExistingMemberDetail.class);
        verify(memberQueryPort).findByProviderAndSocialId(SocialProvider.GOOGLE, "google456");
        verify(saveMemberPort).handleExistingUserData(googleMember, googleProfile);
    }
}