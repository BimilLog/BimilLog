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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    @DisplayName("정상 케이스 - 사용자로 토큰 조회")
    void shouldFindTokenByUser_WhenValidUserProvided() {
        // Given: 조회할 사용자와 예상 결과
        User user = User.builder()
                .id(1L)
                .userName("testUser")
                .build();
                
        Token expectedToken = Token.builder()
                .id(1L)
                .users(user)
                .accessToken("user-access-token")
                .refreshToken("user-refresh-token")
                .build();
                
        given(tokenRepository.findByUsers(user)).willReturn(Optional.of(expectedToken));

        // When: 사용자로 토큰 조회 실행
        Optional<Token> result = tokenAdapter.findByUsers(user);

        // Then: 사용자에 해당하는 토큰이 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getUsers()).isEqualTo(user);
        assertThat(result.get().getAccessToken()).isEqualTo("user-access-token");
        verify(tokenRepository).findByUsers(eq(user));
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
    @DisplayName("경계값 - 토큰이 없는 사용자로 조회")
    void shouldReturnEmpty_WhenUserHasNoToken() {
        // Given: 토큰이 없는 사용자
        User userWithoutToken = User.builder()
                .id(1L)
                .userName("userWithoutToken")
                .build();
                
        given(tokenRepository.findByUsers(userWithoutToken)).willReturn(Optional.empty());

        // When: 토큰이 없는 사용자로 조회 실행
        Optional<Token> result = tokenAdapter.findByUsers(userWithoutToken);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
        verify(tokenRepository).findByUsers(eq(userWithoutToken));
    }

    @Test
    @DisplayName("경계값 - null ID로 토큰 조회")
    void shouldHandleNullId_WhenNullIdProvided() {
        // Given: null ID
        Long nullId = null;
        given(tokenRepository.findById(any())).willReturn(Optional.empty());

        // When: null ID로 토큰 조회 실행
        Optional<Token> result = tokenAdapter.findById(nullId);

        // Then: Repository에 null이 전달되는지 검증
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // null ID에 대한 방어 코드 누락으로 NPE 발생 가능성
        // 수정 필요: TokenAdapter.findById() 메서드에 null 검증 추가
        assertThat(result).isEmpty();
        verify(tokenRepository).findById(nullId);
    }

    @Test
    @DisplayName("경계값 - null 사용자로 토큰 조회")
    void shouldHandleNullUser_WhenNullUserProvided() {
        // Given: null 사용자
        User nullUser = null;
        given(tokenRepository.findByUsers(any())).willReturn(Optional.empty());

        // When: null 사용자로 토큰 조회 실행
        Optional<Token> result = tokenAdapter.findByUsers(nullUser);

        // Then: Repository에 null이 전달되는지 검증
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // null User에 대한 방어 코드 누락으로 NPE 발생 가능성
        // 수정 필요: TokenAdapter.findByUsers() 메서드에 null 검증 추가
        assertThat(result).isEmpty();
        verify(tokenRepository).findByUsers(nullUser);
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
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // null 입력에 대한 방어 코드 누락으로 NPE 발생 가능성
        // 수정 필요: TokenAdapter.save() 메서드에 null 검증 추가
        assertThat(result).isNull();
        verify(tokenRepository).save(nullToken);
    }

    @Test
    @DisplayName("통합 - 토큰 업데이트 시나리오")
    void shouldUpdateToken_WhenTokenAlreadyExists() {
        // Given: 기존 토큰과 업데이트될 토큰
        User user = User.builder()
                .id(1L)
                .userName("testUser")
                .build();
                
        Token existingToken = Token.builder()
                .id(1L)
                .users(user)
                .accessToken("old-access-token")
                .refreshToken("old-refresh-token")
                .build();
                
        Token updatedToken = Token.builder()
                .id(1L)
                .users(user)
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
                
        given(tokenRepository.findByUsers(user)).willReturn(Optional.of(existingToken));
        given(tokenRepository.save(any(Token.class))).willReturn(updatedToken);

        // When: 기존 토큰 조회 후 업데이트
        Optional<Token> foundToken = tokenAdapter.findByUsers(user);
        assertThat(foundToken).isPresent();
        
        Token result = tokenAdapter.save(updatedToken);

        // Then: 토큰이 올바르게 업데이트되는지 검증
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(tokenRepository).findByUsers(eq(user));
        verify(tokenRepository).save(eq(updatedToken));
    }
}