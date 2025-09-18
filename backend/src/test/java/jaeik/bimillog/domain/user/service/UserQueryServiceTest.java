package jaeik.bimillog.domain.user.service;

import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.application.service.UserQueryService;
import jaeik.bimillog.domain.user.entity.*;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.testutil.TestUsers;
import jaeik.bimillog.testutil.TestSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserQueryService 테스트</h2>
 * <p>사용자 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>헥사고날 아키텍처 원칙에 따라 모든 외부 의존성을 Mock으로 격리하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryService 테스트")
class UserQueryServiceTest {

    @Mock
    private UserQueryPort userQueryPort;
    
    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @InjectMocks
    private UserQueryService userQueryService;

    // 테스트 전역 사용자
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = TestUsers.USER1;
        adminUser = TestUsers.ADMIN;
    }

    @Test
    @DisplayName("소셜 정보로 사용자 조회 - 정상 케이스")
    void shouldFindUser_WhenValidProviderAndSocialId() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";

        User expectedUser = User.builder()
                .id(1L)
                .socialId(socialId)
                .provider(provider)
                .userName("testUser")
                .socialNickname(testUser.getSocialNickname())
                .thumbnailImage(testUser.getThumbnailImage())
                .role(UserRole.USER)
                .setting(testUser.getSetting())
                .build();

        given(userQueryPort.findByProviderAndSocialId(provider, socialId))
                .willReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userQueryService.findByProviderAndSocialId(provider, socialId);

        // Then
        verify(userQueryPort).findByProviderAndSocialId(provider, socialId);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedUser);
        assertThat(result.get().getProvider()).isEqualTo(provider);
        assertThat(result.get().getSocialId()).isEqualTo(socialId);
    }

    @Test
    @DisplayName("소셜 정보로 사용자 조회 - 사용자가 존재하지 않는 경우")
    void shouldReturnEmpty_WhenUserNotFoundByProviderAndSocialId() {
        // Given
        SocialProvider provider = SocialProvider.GOOGLE;
        String socialId = "nonexistent";

        given(userQueryPort.findByProviderAndSocialId(provider, socialId))
                .willReturn(Optional.empty());

        // When
        Optional<User> result = userQueryService.findByProviderAndSocialId(provider, socialId);

        // Then
        verify(userQueryPort).findByProviderAndSocialId(provider, socialId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 정상 케이스")
    void shouldFindUser_WhenValidId() {
        // Given
        Long userId = 1L;
        User expectedUser = User.builder()
                .id(userId)
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname(testUser.getSocialNickname())
                .thumbnailImage(testUser.getThumbnailImage())
                .role(UserRole.USER)
                .setting(testUser.getSetting())
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userQueryService.findById(userId);

        // Then
        verify(userQueryPort).findById(userId);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 사용자가 존재하지 않는 경우")
    void shouldReturnEmpty_WhenUserNotFoundById() {
        // Given
        Long nonexistentId = 999L;

        given(userQueryPort.findById(nonexistentId)).willReturn(Optional.empty());

        // When
        Optional<User> result = userQueryService.findById(nonexistentId);

        // Then
        verify(userQueryPort).findById(nonexistentId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 존재하는 닉네임")
    void shouldReturnTrue_WhenUserNameExists() {
        // Given
        String existingUserName = "existingUser";

        given(userQueryPort.existsByUserName(existingUserName)).willReturn(true);

        // When
        boolean result = userQueryService.existsByUserName(existingUserName);

        // Then
        verify(userQueryPort).existsByUserName(existingUserName);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 존재하지 않는 닉네임")
    void shouldReturnFalse_WhenUserNameNotExists() {
        // Given
        String nonexistentUserName = "nonexistentUser";

        given(userQueryPort.existsByUserName(nonexistentUserName)).willReturn(false);

        // When
        boolean result = userQueryService.existsByUserName(nonexistentUserName);

        // Then
        verify(userQueryPort).existsByUserName(nonexistentUserName);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회 - 정상 케이스")
    void shouldFindUser_WhenValidUserName() {
        // Given
        String userName = "testUser";
        User expectedUser = User.builder()
                .id(1L)
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .userName(userName)
                .socialNickname(testUser.getSocialNickname())
                .thumbnailImage(testUser.getThumbnailImage())
                .role(UserRole.USER)
                .setting(testUser.getSetting())
                .build();

        given(userQueryPort.findByUserName(userName)).willReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userQueryService.findByUserName(userName);

        // Then
        verify(userQueryPort).findByUserName(userName);
        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo(userName);
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회 - 사용자가 존재하지 않는 경우")
    void shouldReturnEmpty_WhenUserNotFoundByUserName() {
        // Given
        String nonexistentUserName = "nonexistentUser";

        given(userQueryPort.findByUserName(nonexistentUserName)).willReturn(Optional.empty());

        // When
        Optional<User> result = userQueryService.findByUserName(nonexistentUserName);

        // Then
        verify(userQueryPort).findByUserName(nonexistentUserName);
        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("ID로 사용자 프록시 조회 - 정상 케이스")
    void shouldGetReferenceById_WhenValidUserId() {
        // Given
        Long userId = 1L;
        User proxyUser = User.builder()
                .id(userId)
                .socialId(testUser.getSocialId())
                .provider(testUser.getProvider())
                .userName(testUser.getUserName())
                .socialNickname(testUser.getSocialNickname())
                .thumbnailImage(testUser.getThumbnailImage())
                .role(testUser.getRole())
                .build();

        given(userQueryPort.getReferenceById(userId)).willReturn(proxyUser);

        // When
        User result = userQueryService.getReferenceById(userId);

        // Then
        verify(userQueryPort).getReferenceById(userId);
        assertThat(result).isEqualTo(proxyUser);
        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("토큰 ID로 토큰 조회 - 정상 케이스")
    void shouldFindTokenById_WhenTokenIdExists() {
        // Given
        Long tokenId = 1L;
        Token expectedToken = Token.createTemporaryToken("access-token", "refresh-token");
                

        given(globalTokenQueryPort.findById(tokenId)).willReturn(Optional.of(expectedToken));

        // When
        Optional<Token> result = userQueryService.findTokenById(tokenId);

        // Then
        verify(globalTokenQueryPort).findById(tokenId);
        assertThat(result).isPresent();
        assertThat(result.get().getAccessToken()).isEqualTo("access-token");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token");
    }


    @Test
    @DisplayName("설정 ID로 설정 조회 - 정상 케이스")
    void shouldFindSetting_WhenValidSettingId() {
        // Given
        Long settingId = 1L;
        Setting expectedSetting = TestSettings.copyWithId(TestSettings.custom(true, false, true), settingId);

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(expectedSetting));

        // When
        Setting result = userQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isEqualTo(expectedSetting);
        assertThat(result.getId()).isEqualTo(settingId);
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("설정 ID로 설정 조회 - 설정이 존재하지 않는 경우")
    void shouldThrowException_WhenSettingNotFound() {
        // Given
        Long settingId = 999L;

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userQueryService.findBySettingId(settingId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.SETTINGS_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findSettingById(settingId);
    }

}