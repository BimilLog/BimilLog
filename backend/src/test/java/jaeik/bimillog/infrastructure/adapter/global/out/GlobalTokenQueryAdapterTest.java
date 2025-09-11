package jaeik.bimillog.infrastructure.adapter.global.out;

import jaeik.bimillog.domain.user.entity.Token;
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
import static org.mockito.Mockito.*;

/**
 * <h2>GlobalTokenQueryAdapter 단위 테스트</h2>
 * <p>토큰 조회 공용 어댑터의 동작을 검증하는 단위 테스트입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalTokenQueryAdapter 단위 테스트")
class GlobalTokenQueryAdapterTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private Token token;

    @InjectMocks
    private GlobalTokenQueryAdapter globalTokenQueryAdapter;

    @Test
    @DisplayName("토큰 ID로 조회 - 토큰 존재")
    void shouldReturnToken_WhenTokenExists() {
        // Given
        Long tokenId = 1L;
        given(tokenRepository.findById(tokenId)).willReturn(Optional.of(token));

        // When
        Optional<Token> result = globalTokenQueryAdapter.findById(tokenId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(token);
        verify(tokenRepository, times(1)).findById(tokenId);
    }

    @Test
    @DisplayName("토큰 ID로 조회 - 토큰 없음")
    void shouldReturnEmpty_WhenTokenNotExists() {
        // Given
        Long tokenId = 999L;
        given(tokenRepository.findById(tokenId)).willReturn(Optional.empty());

        // When
        Optional<Token> result = globalTokenQueryAdapter.findById(tokenId);

        // Then
        assertThat(result).isEmpty();
        verify(tokenRepository, times(1)).findById(tokenId);
    }

    @Test
    @DisplayName("토큰 ID로 조회 - null ID 처리")
    void shouldReturnEmpty_WhenTokenIdIsNull() {
        // When
        Optional<Token> result = globalTokenQueryAdapter.findById(null);

        // Then
        assertThat(result).isEmpty();
        verify(tokenRepository, never()).findById(any());
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 조회 - 토큰 목록 존재")
    void shouldReturnTokenList_WhenUserHasTokens() {
        // Given
        Long userId = 1L;
        List<Token> tokens = Arrays.asList(token, mock(Token.class));
        given(tokenRepository.findByUsersId(userId)).willReturn(tokens);

        // When
        List<Token> result = globalTokenQueryAdapter.findAllByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(token);
        verify(tokenRepository, times(1)).findByUsersId(userId);
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 조회 - 토큰 없음")
    void shouldReturnEmptyList_WhenUserHasNoTokens() {
        // Given
        Long userId = 1L;
        given(tokenRepository.findByUsersId(userId)).willReturn(List.of());

        // When
        List<Token> result = globalTokenQueryAdapter.findAllByUserId(userId);

        // Then
        assertThat(result).isEmpty();
        verify(tokenRepository, times(1)).findByUsersId(userId);
    }
}