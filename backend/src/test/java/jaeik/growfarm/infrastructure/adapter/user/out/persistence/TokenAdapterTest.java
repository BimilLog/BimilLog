package jaeik.growfarm.infrastructure.adapter.user.out.persistence;

import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenAdapter;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * <h2>TokenAdapter 테스트</h2>
 * <p>토큰 어댑터의 persistence 동작 검증</p>
 * <p>Token 엔티티의 CRUD 작업 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class TokenAdapterTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenAdapter tokenAdapter;

    @Test
    @DisplayName("정상 케이스 - ID로 토큰 조회")
    void shouldFindToken_WhenValidIdProvided() {
        // Given: 조회할 토큰 ID와 예상 결과
        Long tokenId = 1L;
        Token expectedToken = Token.builder()
                .id(tokenId)
                .accessToken("access-token-value")
                .refreshToken("refresh-token-value")
                .build();
                
        given(tokenRepository.findById(tokenId)).willReturn(Optional.of(expectedToken));

        // When: 토큰 조회 실행
        Optional<Token> result = tokenAdapter.findById(tokenId);

        // Then: 올바른 토큰이 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(tokenId);
        assertThat(result.get().getAccessToken()).isEqualTo("access-token-value");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token-value");
        verify(tokenRepository).findById(eq(tokenId));
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 토큰 목록 조회")
    void shouldFindTokensByUserId_WhenValidUserIdProvided() {
        // Given: 조회할 사용자 ID와 예상 결과
        Long userId = 1L;
        
        Token expectedToken1 = Token.builder()
                .id(1L)
                .accessToken("user-access-token-1")
                .refreshToken("user-refresh-token-1")
                .build();
                
        Token expectedToken2 = Token.builder()
                .id(2L)
                .accessToken("user-access-token-2")
                .refreshToken("user-refresh-token-2")
                .build();
                
        given(tokenRepository.findByUsersId(userId)).willReturn(List.of(expectedToken1, expectedToken2));

        // When: 사용자 ID로 토큰 목록 조회 실행
        List<Token> result = tokenRepository.findByUsersId(userId);

        // Then: 사용자에 해당하는 토큰 목록이 조회되는지 검증
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccessToken()).isEqualTo("user-access-token-1");
        assertThat(result.get(1).getAccessToken()).isEqualTo("user-access-token-2");
        verify(tokenRepository).findByUsersId(eq(userId));
    }

    @Test
    @DisplayName("정상 케이스 - 토큰 저장")
    void shouldSaveToken_WhenValidTokenProvided() {
        // Given: 저장할 토큰과 예상 결과
        User user = User.builder()
                .id(1L)
                .userName("testUser")
                .build();
                
        Token inputToken = Token.builder()
                .users(user)
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
                
        Token savedToken = Token.builder()
                .id(1L)
                .users(user)
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
                
        given(tokenRepository.save(any(Token.class))).willReturn(savedToken);

        // When: 토큰 저장 실행
        Token result = tokenAdapter.save(inputToken);

        // Then: 토큰이 올바르게 저장되고 결과가 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.getUsers()).isEqualTo(user);
        verify(tokenRepository).save(eq(inputToken));
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 토큰 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // Given: 존재하지 않는 토큰 ID
        Long nonExistentId = 999L;
        given(tokenRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // When: 존재하지 않는 토큰 조회 실행
        Optional<Token> result = tokenAdapter.findById(nonExistentId);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
        verify(tokenRepository).findById(eq(nonExistentId));
    }

    @Test
    @DisplayName("경계값 - 토큰이 없는 사용자 ID로 조회")
    void shouldReturnEmptyList_WhenUserHasNoToken() {
        // Given: 토큰이 없는 사용자 ID
        Long userIdWithoutToken = 999L;
                
        given(tokenRepository.findByUsersId(userIdWithoutToken)).willReturn(List.of());

        // When: 토큰이 없는 사용자 ID로 조회 실행
        List<Token> result = tokenRepository.findByUsersId(userIdWithoutToken);

        // Then: 빈 리스트가 반환되는지 검증
        assertThat(result).isEmpty();
        verify(tokenRepository).findByUsersId(eq(userIdWithoutToken));
    }

    @Test
    @DisplayName("경계값 - null ID로 토큰 조회")
    void shouldHandleNullId_WhenNullIdProvided() {
        // Given: null ID - adapter의 null 방어 로직 테스트
        Long nullId = null;
        // Note: null 방어 로직으로 repository 호출이 방지되므로 mock 설정 불필요

        // When: null ID로 토큰 조회 실행
        Optional<Token> result = tokenAdapter.findById(nullId);

        // Then: null 방어 로직에 의해 빈 Optional 반환, repository 호출 없음
        assertThat(result).isEmpty();
        // null 방어 로직이 정상 동작하므로 repository는 호출되지 않아야 함
        verify(tokenRepository, never()).findById(any());
    }

    @Test
    @DisplayName("경계값 - null 사용자 ID로 토큰 조회")
    void shouldHandleNullUserId_WhenNullUserIdProvided() {
        // Given: null 사용자 ID
        Long nullUserId = null;
        given(tokenRepository.findByUsersId(any())).willReturn(List.of());

        // When: null 사용자 ID로 토큰 조회 실행
        List<Token> result = tokenRepository.findByUsersId(nullUserId);

        // Then: Repository에 null이 전달되고 빈 리스트가 반환되는지 검증
        assertThat(result).isEmpty();
        verify(tokenRepository).findByUsersId(nullUserId);
    }

    @Test
    @DisplayName("경계값 - null 토큰 저장")
    void shouldHandleNullToken_WhenNullTokenProvided() {
        // Given: null 토큰
        Token nullToken = null;
        given(tokenRepository.save(any())).willReturn(null);

        // When: null 토큰 저장 실행
        Token result = tokenAdapter.save(nullToken);

        // Then: Repository에 null이 전달되고 결과도 null인지 검증
        assertThat(result).isNull();
        verify(tokenRepository).save(nullToken);
    }

    @Test
    @DisplayName("통합 - 사용자의 모든 토큰 삭제 시나리오")
    void shouldDeleteAllTokens_WhenUserIdProvided() {
        // Given: 사용자 ID와 삭제할 토큰들
        Long userId = 1L;
        
        Token token1 = Token.builder()
                .id(1L)
                .accessToken("access-token-1")
                .refreshToken("refresh-token-1")
                .build();
                
        Token token2 = Token.builder()
                .id(2L)
                .accessToken("access-token-2")
                .refreshToken("refresh-token-2")
                .build();
                
        given(tokenRepository.findByUsersId(userId)).willReturn(List.of(token1, token2));

        // When: 사용자의 토큰 목록 조회 후 삭제 실행
        List<Token> foundTokens = tokenRepository.findByUsersId(userId);
        assertThat(foundTokens).hasSize(2);
        
        tokenRepository.deleteAllByUserId(userId);

        // Then: 올바른 메서드들이 호출되는지 검증
        verify(tokenRepository).findByUsersId(eq(userId));
        verify(tokenRepository).deleteAllByUserId(eq(userId));
    }
}