package jaeik.bimillog.infrastructure.adapter.auth.auth;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.AuthToTokenAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.TokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>AuthToTokenAdapter 단위 테스트</h2>
 * <p>토큰 어댑터의 핵심 비즈니스 로직을 Mock으로 검증</p>
 * <p>Repository 래핑 기능과 데이터 전달 로직 중점 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthToTokenAdapterTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private AuthToTokenAdapter authToTokenAdapter;

    @Test
    @DisplayName("토큰 ID로 조회 - 정상 케이스")
    void shouldFindTokenById_WhenTokenExists() {
        // Given
        Long tokenId = 1L;
        Token expectedToken = createTestToken(tokenId);
        given(tokenRepository.findById(tokenId)).willReturn(Optional.of(expectedToken));

        // When
        Optional<Token> result = authToTokenAdapter.findById(tokenId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedToken);
        verify(tokenRepository).findById(tokenId);
    }

    @Test
    @DisplayName("토큰 ID로 조회 - 토큰 없음")
    void shouldReturnEmpty_WhenTokenNotExists() {
        // Given
        Long nonExistentId = 999L;
        given(tokenRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // When
        Optional<Token> result = authToTokenAdapter.findById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
        verify(tokenRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 조회 - 정상 케이스")
    void shouldFindAllTokensByUserId_WhenTokensExist() {
        // Given
        Long userId = 1L;
        List<Token> expectedTokens = Arrays.asList(
            createTestToken(1L),
            createTestToken(2L)
        );
        given(tokenRepository.findByUsersId(userId)).willReturn(expectedTokens);

        // When
        List<Token> result = authToTokenAdapter.findAllByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedTokens);
        verify(tokenRepository).findByUsersId(userId);
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 조회 - 토큰 없음")
    void shouldReturnEmptyList_WhenUserHasNoTokens() {
        // Given
        Long userId = 1L;
        given(tokenRepository.findByUsersId(userId)).willReturn(List.of());

        // When
        List<Token> result = authToTokenAdapter.findAllByUserId(userId);

        // Then
        assertThat(result).isEmpty();
        verify(tokenRepository).findByUsersId(userId);
    }

    private Token createTestToken(Long tokenId) {
        User user = createTestUser();
        return Token.createToken("access-token", "refresh-token", user);
    }

    private User createTestUser() {
        Setting setting = Setting.builder()
                .commentNotification(true)
                .messageNotification(true)
                .postFeaturedNotification(true)
                .build();

        return User.builder()
                .id(1L)
                .userName("testUser")
                .socialId("123456789")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .socialNickname("testNickname")
                .thumbnailImage("https://example.com/profile.jpg")
                .setting(setting)
                .build();
    }
}