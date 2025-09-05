package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.out.TokenPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private TokenPort tokenPort;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    @DisplayName("소셜 정보로 사용자 조회 - 정상 케이스")
    void shouldFindUser_WhenValidProviderAndSocialId() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        
        User expectedUser = User.builder()
                .id(1L)
                .userName("testUser")
                .provider(provider)
                .socialId(socialId)
                .role(UserRole.USER)
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
                .userName("testUser")
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
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
                .userName(userName)
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
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
        Token expectedToken = Token.builder()
                .id(tokenId)
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .build();

        given(tokenPort.findById(tokenId)).willReturn(Optional.of(expectedToken));

        // When
        Optional<Token> result = userQueryService.findTokenById(tokenId);

        // Then
        verify(tokenPort).findById(tokenId);
        assertThat(result).isPresent();
        assertThat(result.get().getAccessToken()).isEqualTo("access_token");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh_token");
    }


    @Test
    @DisplayName("모든 소셜 제공자에 대한 사용자 조회")
    void shouldFindUser_ForAllSocialProviders() {
        // Given & When & Then
        SocialProvider[] providers = SocialProvider.values();
        
        for (SocialProvider provider : providers) {
            String socialId = "test_" + provider.name().toLowerCase();
            User expectedUser = User.builder()
                    .id(1L)
                    .userName("testUser")
                    .provider(provider)
                    .socialId(socialId)
                    .role(UserRole.USER)
                    .build();

            given(userQueryPort.findByProviderAndSocialId(provider, socialId))
                    .willReturn(Optional.of(expectedUser));

            Optional<User> result = userQueryService.findByProviderAndSocialId(provider, socialId);

            assertThat(result).isPresent();
            assertThat(result.get().getProvider()).isEqualTo(provider);
            assertThat(result.get().getSocialId()).isEqualTo(socialId);
        }
    }

    @Test
    @DisplayName("null 값들로 조회 시도")
    void shouldHandleNullValues_Gracefully() {
        // Given
        given(userQueryPort.findByProviderAndSocialId(null, null)).willReturn(Optional.empty());
        given(userQueryPort.findByUserName(null)).willReturn(Optional.empty());
        given(userQueryPort.existsByUserName(null)).willReturn(false);

        // When & Then
        Optional<User> result1 = userQueryService.findByProviderAndSocialId(null, null);
        Optional<User> result2 = userQueryService.findByUserName(null);
        boolean result3 = userQueryService.existsByUserName(null);

        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
        assertThat(result3).isFalse();
    }
}