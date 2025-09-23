package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.TokenRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
@DisplayName("GlobalTokenQueryAdapter 단위 테스트")
class GlobalTokenQueryAdapterTest extends BaseUnitTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private GlobalTokenQueryAdapter globalTokenQueryAdapter;
    
    private Token testToken;
    private Token adminToken;

    @Override
    protected void setUpChild() {
        // 실제 Token 객체 생성
        testToken = Token.createToken("test-access", "test-refresh", testUser);
        adminToken = Token.createToken("admin-access", "admin-refresh", adminUser);
    }

    @Test
    @DisplayName("토큰 ID로 조회 - 토큰 존재")
    void shouldReturnToken_WhenTokenExists() {
        // Given
        Long tokenId = 1L;
        given(tokenRepository.findById(tokenId)).willReturn(Optional.of(testToken));

        // When
        Optional<Token> result = globalTokenQueryAdapter.findById(tokenId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testToken);
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
        // Given
        given(tokenRepository.findById(null)).willReturn(Optional.empty());

        // When
        Optional<Token> result = globalTokenQueryAdapter.findById(null);

        // Then
        assertThat(result).isEmpty();
        verify(tokenRepository, times(1)).findById(null);
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 조회 - 토큰 목록 존재")
    void shouldReturnTokenList_WhenUserHasTokens() {
        // Given
        Long userId = 1L;
        Token temporaryToken = Token.createTemporaryToken("temp-access", "temp-refresh");
        List<Token> tokens = Arrays.asList(testToken, temporaryToken);
        given(tokenRepository.findByUsersId(userId)).willReturn(tokens);

        // When
        List<Token> result = globalTokenQueryAdapter.findAllByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(testToken, temporaryToken);
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