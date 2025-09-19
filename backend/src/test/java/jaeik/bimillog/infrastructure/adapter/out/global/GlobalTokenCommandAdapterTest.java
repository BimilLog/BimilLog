package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.TokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>GlobalTokenCommandAdapter 단위 테스트</h2>
 * <p>토큰 명령 공용 어댑터의 동작을 검증하는 단위 테스트입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalTokenCommandAdapter 단위 테스트")
class GlobalTokenCommandAdapterTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private Token token;

    @InjectMocks
    private GlobalTokenCommandAdapter globalTokenCommandAdapter;

    @Test
    @DisplayName("토큰 저장 - 성공")
    void shouldSaveToken_WhenValidTokenProvided() {
        // Given
        given(tokenRepository.save(token)).willReturn(token);

        // When
        Token result = globalTokenCommandAdapter.save(token);

        // Then
        assertThat(result).isEqualTo(token);
        verify(tokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 삭제 - 성공")
    void shouldDeleteAllTokensByUserId_WhenUserIdProvided() {
        // Given
        Long userId = 1L;

        // When
        globalTokenCommandAdapter.deleteAllByUserId(userId);

        // Then
        verify(tokenRepository, times(1)).deleteAllByUserId(userId);
    }
}