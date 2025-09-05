package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.AuthPort;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.JwtInvalidatePort;
import jaeik.bimillog.domain.auth.application.port.out.LoadTokenPort;
import jaeik.bimillog.domain.auth.application.service.TokenBlacklistService;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>TokenBlacklistService 단위 테스트</h2>
 * <p>토큰 블랙리스트 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService 단위 테스트")
class TokenBlacklistServiceTest {

    @Mock
    private JwtInvalidatePort jwtInvalidatePort;

    @Mock
    private BlacklistPort blacklistPort;

    @Mock
    private LoadTokenPort loadTokenPort;

    @Mock
    private AuthPort authPort;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String testToken;
    private String testTokenHash;
    private User testUser;
    private Token testToken1;
    private Token testToken2;

    @BeforeEach
    void setUp() {
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        testTokenHash = "hash123abc";
        
        testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testToken1 = Token.createTemporaryToken("access-token-1", "refresh-token-1");
                

        testToken2 = Token.createTemporaryToken("access-token-2", "refresh-token-2");
                
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 있는 경우 true 반환")
    void shouldReturnTrue_WhenTokenIsBlacklisted() {
        // Given
        given(authPort.generateTokenHash(testToken)).willReturn(testTokenHash);
        given(jwtInvalidatePort.isBlacklisted(testTokenHash)).willReturn(true);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isTrue();
        verify(authPort).generateTokenHash(testToken);
        verify(jwtInvalidatePort).isBlacklisted(testTokenHash);
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 없는 경우 false 반환")
    void shouldReturnFalse_WhenTokenIsNotBlacklisted() {
        // Given
        given(authPort.generateTokenHash(testToken)).willReturn(testTokenHash);
        given(jwtInvalidatePort.isBlacklisted(testTokenHash)).willReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isFalse();
        verify(authPort).generateTokenHash(testToken);
        verify(jwtInvalidatePort).isBlacklisted(testTokenHash);
    }

    @Test
    @DisplayName("토큰 해시 생성 실패 시 안전하게 true 반환")
    void shouldReturnTrue_WhenTokenHashGenerationFails() {
        // Given
        doThrow(new RuntimeException("Hash generation failed"))
                .when(authPort).generateTokenHash(testToken);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isTrue();
        verify(authPort).generateTokenHash(testToken);
        verify(jwtInvalidatePort, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("블랙리스트 확인 실패 시 안전하게 true 반환")
    void shouldReturnTrue_WhenBlacklistCheckFails() {
        // Given
        given(authPort.generateTokenHash(testToken)).willReturn(testTokenHash);
        doThrow(new RuntimeException("Cache check failed"))
                .when(jwtInvalidatePort).isBlacklisted(testTokenHash);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isTrue();
        verify(authPort).generateTokenHash(testToken);
        verify(jwtInvalidatePort).isBlacklisted(testTokenHash);
    }

    @Test
    @DisplayName("사용자의 모든 토큰을 블랙리스트에 등록 성공")
    void shouldBlacklistAllUserTokens_WhenUserHasTokens() {
        // Given
        Long userId = 100L;
        String reason = "Security violation";
        List<Token> userTokens = List.of(testToken1, testToken2);
        
        given(loadTokenPort.findAllByUserId(userId)).willReturn(userTokens);
        given(authPort.generateTokenHash("access-token-1")).willReturn("hash1");
        given(authPort.generateTokenHash("access-token-2")).willReturn("hash2");

        // When
        tokenBlacklistService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(loadTokenPort).findAllByUserId(userId);
        verify(authPort).generateTokenHash("access-token-1");
        verify(authPort).generateTokenHash("access-token-2");
        
        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        
        verify(jwtInvalidatePort).blacklistTokenHashes(
                hashesCaptor.capture(), 
                eq(reason), 
                durationCaptor.capture()
        );
        
        List<String> capturedHashes = hashesCaptor.getValue();
        assertThat(capturedHashes).containsExactlyInAnyOrder("hash1", "hash2");
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("사용자에게 활성 토큰이 없는 경우 처리")
    void shouldHandleGracefully_WhenUserHasNoTokens() {
        // Given
        Long userId = 100L;
        String reason = "Security violation";
        given(loadTokenPort.findAllByUserId(userId)).willReturn(List.of());

        // When
        tokenBlacklistService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(loadTokenPort).findAllByUserId(userId);
        verify(authPort, never()).generateTokenHash(anyString());
        verify(jwtInvalidatePort, never()).blacklistTokenHashes(any(), anyString(), any());
    }

    @Test
    @DisplayName("토큰 해시 생성 일부 실패 시 유효한 해시만 블랙리스트에 등록")
    void shouldBlacklistValidHashes_WhenSomeTokenHashGenerationFails() {
        // Given
        Long userId = 100L;
        String reason = "Partial failure test";
        List<Token> userTokens = List.of(testToken1, testToken2);
        
        given(loadTokenPort.findAllByUserId(userId)).willReturn(userTokens);
        given(authPort.generateTokenHash("access-token-1")).willReturn("hash1");
        doThrow(new RuntimeException("Hash generation failed"))
                .when(authPort).generateTokenHash("access-token-2");

        // When
        tokenBlacklistService.blacklistAllUserTokens(userId, reason);

        // Then
        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        verify(jwtInvalidatePort).blacklistTokenHashes(
                hashesCaptor.capture(), 
                eq(reason), 
                any(Duration.class)
        );
        
        List<String> capturedHashes = hashesCaptor.getValue();
        assertThat(capturedHashes).containsExactly("hash1");
    }

    @Test
    @DisplayName("모든 토큰 해시 생성 실패 시 블랙리스트 등록하지 않음")
    void shouldNotBlacklist_WhenAllTokenHashGenerationFails() {
        // Given
        Long userId = 100L;
        String reason = "Complete failure test";
        List<Token> userTokens = List.of(testToken1, testToken2);
        
        given(loadTokenPort.findAllByUserId(userId)).willReturn(userTokens);
        doThrow(new RuntimeException("Hash generation failed"))
                .when(authPort).generateTokenHash(anyString());

        // When
        tokenBlacklistService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(loadTokenPort).findAllByUserId(userId);
        verify(authPort).generateTokenHash("access-token-1");
        verify(authPort).generateTokenHash("access-token-2");
        verify(jwtInvalidatePort, never()).blacklistTokenHashes(any(), anyString(), any());
    }

    @Test
    @DisplayName("사용자 토큰 조회 실패 시 예외 처리")
    void shouldHandleException_WhenLoadingUserTokensFails() {
        // Given
        Long userId = 100L;
        String reason = "Load failure test";
        doThrow(new RuntimeException("Token loading failed"))
                .when(loadTokenPort).findAllByUserId(userId);

        // When
        tokenBlacklistService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(loadTokenPort).findAllByUserId(userId);
        verify(authPort, never()).generateTokenHash(anyString());
        verify(jwtInvalidatePort, never()).blacklistTokenHashes(any(), anyString(), any());
    }

    @Test
    @DisplayName("다양한 블랙리스트 등록 사유 테스트")
    void shouldHandleDifferentReasons() {
        // Given
        String[] reasons = {"Security violation", "Account suspended", "Force logout", "Admin action"};
        Long userId = 100L;
        List<Token> userTokens = List.of(testToken1);
        
        given(loadTokenPort.findAllByUserId(userId)).willReturn(userTokens);
        given(authPort.generateTokenHash("access-token-1")).willReturn("hash1");

        for (String reason : reasons) {
            // When
            tokenBlacklistService.blacklistAllUserTokens(userId, reason);

            // Then
            verify(jwtInvalidatePort).blacklistTokenHashes(
                    eq(List.of("hash1")), 
                    eq(reason), 
                    any(Duration.class)
            );
        }
    }

    @Test
    @DisplayName("빈 문자열 토큰 해시 확인")
    void shouldHandleEmptyToken() {
        // Given
        String emptyToken = "";
        given(authPort.generateTokenHash(emptyToken)).willReturn("");
        given(jwtInvalidatePort.isBlacklisted("")).willReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(emptyToken);

        // Then
        assertThat(result).isFalse();
        verify(authPort).generateTokenHash(emptyToken);
        verify(jwtInvalidatePort).isBlacklisted("");
    }

    @Test
    @DisplayName("null 토큰 처리")
    void shouldHandleNullToken() {
        // Given
        String nullToken = null;
        doThrow(new RuntimeException("Null token"))
                .when(authPort).generateTokenHash(nullToken);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(nullToken);

        // Then
        assertThat(result).isTrue();
        verify(authPort).generateTokenHash(nullToken);
    }
}