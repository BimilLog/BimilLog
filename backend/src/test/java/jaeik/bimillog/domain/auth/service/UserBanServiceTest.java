package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.RedisJwtBlacklistPort;
import jaeik.bimillog.domain.auth.application.service.UserBanService;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.testutil.BaseAuthUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>UserBanService 단위 테스트</h2>
 * <p>토큰 블랙리스트 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("UserBanService 단위 테스트")
class UserBanServiceTest extends BaseAuthUnitTest {

    @Mock
    private GlobalJwtPort globalJwtPort;

    @Mock
    private RedisJwtBlacklistPort redisJwtBlacklistPort;

    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @InjectMocks
    private UserBanService userBanService;

    private String testTokenString;
    private String testTokenHash;
    private List<Token> testTokenList;

    @BeforeEach
    void setUp() {
        testTokenString = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.TemporaryToken";
        testTokenHash = "hash123abc";
        
        // 테스트용 토큰 생성
        testTokenList = createMultipleTokens(2);
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 있는 경우 true 반환")
    void shouldReturnTrue_WhenTokenIsBlacklisted() {
        // Given
        given(globalJwtPort.generateTokenHash(testTokenString)).willReturn(testTokenHash);
        given(redisJwtBlacklistPort.isBlacklisted(testTokenHash)).willReturn(true);

        // When
        boolean result = userBanService.isBlacklisted(testTokenString);

        // Then
        assertThat(result).isTrue();
        verify(globalJwtPort).generateTokenHash(testTokenString);
        verify(redisJwtBlacklistPort).isBlacklisted(testTokenHash);
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 없는 경우 false 반환")
    void shouldReturnFalse_WhenTokenIsNotBlacklisted() {
        // Given
        given(globalJwtPort.generateTokenHash(testTokenString)).willReturn(testTokenHash);
        given(redisJwtBlacklistPort.isBlacklisted(testTokenHash)).willReturn(false);

        // When
        boolean result = userBanService.isBlacklisted(testTokenString);

        // Then
        assertThat(result).isFalse();
        verify(globalJwtPort).generateTokenHash(testTokenString);
        verify(redisJwtBlacklistPort).isBlacklisted(testTokenHash);
    }

    @Test
    @DisplayName("토큰 해시 생성 실패 시 안전하게 false 반환")
    void shouldReturnFalse_WhenTokenHashGenerationFails() {
        // Given
        doThrow(new RuntimeException("Hash generation failed"))
                .when(globalJwtPort).generateTokenHash(testTokenString);

        // When
        boolean result = userBanService.isBlacklisted(testTokenString);

        // Then
        assertThat(result).isFalse();
        verify(globalJwtPort).generateTokenHash(testTokenString);
        verify(redisJwtBlacklistPort, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("블랙리스트 확인 실패 시 안전하게 false 반환")
    void shouldReturnFalse_WhenBlacklistCheckFails() {
        // Given
        given(globalJwtPort.generateTokenHash(testTokenString)).willReturn(testTokenHash);
        doThrow(new RuntimeException("Cache check failed"))
                .when(redisJwtBlacklistPort).isBlacklisted(testTokenHash);

        // When
        boolean result = userBanService.isBlacklisted(testTokenString);

        // Then
        assertThat(result).isFalse();
        verify(globalJwtPort).generateTokenHash(testTokenString);
        verify(redisJwtBlacklistPort).isBlacklisted(testTokenHash);
    }

    @Test
    @DisplayName("사용자의 모든 토큰을 블랙리스트에 등록 성공")
    void shouldBlacklistAllUserTokens_WhenUserHasTokens() {
        // Given
        Long userId = getTestUser().getId() != null ? getTestUser().getId() : 100L;
        String reason = "Security violation";
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(testTokenList);
        given(globalJwtPort.generateTokenHash("access-token-0")).willReturn("hash0");
        given(globalJwtPort.generateTokenHash("access-token-1")).willReturn("hash1");

        // When
        userBanService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort).generateTokenHash("access-token-0");
        verify(globalJwtPort).generateTokenHash("access-token-1");

        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(redisJwtBlacklistPort).blacklistTokenHashes(
                hashesCaptor.capture(),
                eq(reason),
                durationCaptor.capture()
        );
        
        List<String> capturedHashes = hashesCaptor.getValue();
        assertThat(capturedHashes).containsExactlyInAnyOrder("hash0", "hash1");
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("사용자에게 활성 토큰이 없는 경우 처리")
    void shouldHandleGracefully_WhenUserHasNoTokens() {
        // Given
        Long userId = 100L;
        String reason = "Security violation";
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(List.of());

        // When
        userBanService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort, never()).generateTokenHash(anyString());
        verify(redisJwtBlacklistPort, never()).blacklistTokenHashes(any(), anyString(), any());
    }

    @Test
    @DisplayName("토큰 해시 생성 일부 실패 시 유효한 해시만 블랙리스트에 등록")
    void shouldBlacklistValidHashes_WhenSomeTokenHashGenerationFails() {
        // Given
        Long userId = 100L;
        String reason = "Partial failure test";
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(testTokenList);
        given(globalJwtPort.generateTokenHash("access-token-0")).willReturn("hash0");
        doThrow(new RuntimeException("Hash generation failed"))
                .when(globalJwtPort).generateTokenHash("access-token-1");

        // When
        userBanService.blacklistAllUserTokens(userId, reason);

        // Then
        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisJwtBlacklistPort).blacklistTokenHashes(
                hashesCaptor.capture(),
                eq(reason),
                any(Duration.class)
        );
        
        List<String> capturedHashes = hashesCaptor.getValue();
        assertThat(capturedHashes).containsExactly("hash0");
    }

    @Test
    @DisplayName("모든 토큰 해시 생성 실패 시 블랙리스트 등록하지 않음")
    void shouldNotBlacklist_WhenAllTokenHashGenerationFails() {
        // Given
        Long userId = 100L;
        String reason = "Complete failure test";
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(testTokenList);
        doThrow(new RuntimeException("Hash generation failed"))
                .when(globalJwtPort).generateTokenHash(anyString());

        // When
        userBanService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort).generateTokenHash("access-token-0");
        verify(globalJwtPort).generateTokenHash("access-token-1");
        verify(redisJwtBlacklistPort, never()).blacklistTokenHashes(any(), anyString(), any());
    }

    @Test
    @DisplayName("사용자 토큰 조회 실패 시 예외 처리")
    void shouldHandleException_WhenLoadingUserTokensFails() {
        // Given
        Long userId = 100L;
        String reason = "Load failure test";
        doThrow(new RuntimeException("Token loading failed"))
                .when(globalTokenQueryPort).findAllByUserId(userId);

        // When
        userBanService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort, never()).generateTokenHash(anyString());
        verify(redisJwtBlacklistPort, never()).blacklistTokenHashes(any(), anyString(), any());
    }



    @Test
    @DisplayName("많은 수의 토큰을 블랙리스트에 등록")
    void shouldHandleManyTokens() {
        // Given
        Long userId = 100L;
        String reason = "Many tokens test";
        List<Token> manyTokens = createMultipleTokens(10);
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(manyTokens);
        for (int i = 0; i < 10; i++) {
            given(globalJwtPort.generateTokenHash("access-token-" + i)).willReturn("hash" + i);
        }

        // When
        userBanService.blacklistAllUserTokens(userId, reason);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        for (int i = 0; i < 10; i++) {
            verify(globalJwtPort).generateTokenHash("access-token-" + i);
        }

        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisJwtBlacklistPort).blacklistTokenHashes(
                hashesCaptor.capture(),
                eq(reason),
                any(Duration.class)
        );
        
        List<String> capturedHashes = hashesCaptor.getValue();
        assertThat(capturedHashes).hasSize(10);
    }


}