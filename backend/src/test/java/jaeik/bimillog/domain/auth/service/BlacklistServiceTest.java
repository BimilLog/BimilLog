package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisJwtBlacklistPort;
import jaeik.bimillog.domain.auth.application.service.BlacklistService;
import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.domain.auth.entity.JwtToken;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>BlacklistService 단위 테스트</h2>
 * <p>토큰 블랙리스트 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("BlacklistService 단위 테스트")
@Tag("test")
class BlacklistServiceTest extends BaseUnitTest {

    @Mock
    private GlobalJwtPort globalJwtPort;

    @Mock
    private RedisJwtBlacklistPort redisJwtBlacklistPort;

    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @Mock
    private BlacklistPort blacklistPort;

    @InjectMocks
    private BlacklistService blacklistService;

    private String testTokenString;
    private String testTokenHash;
    private List<JwtToken> testJwtTokenList;

    @BeforeEach
    void setUp() {
        testTokenString = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.TemporaryToken";
        testTokenHash = "hash123abc";
        
        // 테스트용 토큰 생성
        testJwtTokenList = createMultipleTokens(2);
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 있는 경우 true 반환")
    void shouldReturnTrue_WhenTokenIsBlacklisted() {
        // Given
        given(globalJwtPort.generateTokenHash(testTokenString)).willReturn(testTokenHash);
        given(redisJwtBlacklistPort.isBlacklisted(testTokenHash)).willReturn(true);

        // When
        boolean result = blacklistService.isBlacklisted(testTokenString);

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
        boolean result = blacklistService.isBlacklisted(testTokenString);

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
        boolean result = blacklistService.isBlacklisted(testTokenString);

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
        boolean result = blacklistService.isBlacklisted(testTokenString);

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
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(testJwtTokenList);
        given(globalJwtPort.generateTokenHash("access-token-0")).willReturn("hash0");
        given(globalJwtPort.generateTokenHash("access-token-1")).willReturn("hash1");

        // When
        blacklistService.blacklistAllUserTokens(userId);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort).generateTokenHash("access-token-0");
        verify(globalJwtPort).generateTokenHash("access-token-1");

        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(redisJwtBlacklistPort).blacklistTokenHashes(
                hashesCaptor.capture(),
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
        blacklistService.blacklistAllUserTokens(userId);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort, never()).generateTokenHash(anyString());
        verify(redisJwtBlacklistPort, never()).blacklistTokenHashes(any(), any());
    }

    @Test
    @DisplayName("토큰 해시 생성 일부 실패 시 유효한 해시만 블랙리스트에 등록")
    void shouldBlacklistValidHashes_WhenSomeTokenHashGenerationFails() {
        // Given
        Long userId = 100L;
        String reason = "Partial failure test";
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(testJwtTokenList);
        given(globalJwtPort.generateTokenHash("access-token-0")).willReturn("hash0");
        doThrow(new RuntimeException("Hash generation failed"))
                .when(globalJwtPort).generateTokenHash("access-token-1");

        // When
        blacklistService.blacklistAllUserTokens(userId);

        // Then
        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisJwtBlacklistPort).blacklistTokenHashes(
                hashesCaptor.capture(),
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
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(testJwtTokenList);
        doThrow(new RuntimeException("Hash generation failed"))
                .when(globalJwtPort).generateTokenHash(anyString());

        // When
        blacklistService.blacklistAllUserTokens(userId);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort).generateTokenHash("access-token-0");
        verify(globalJwtPort).generateTokenHash("access-token-1");
        verify(redisJwtBlacklistPort, never()).blacklistTokenHashes(any(), any());
    }

    @Test
    @DisplayName("사용자 토큰 조회 실패 시 예외 처리")
    void shouldHandleException_WhenLoadingUserTokensFails() {
        // Given
        Long userId = 100L;
        String reason = "Load failure test";
        doThrow(new RuntimeException("JwtToken loading failed"))
                .when(globalTokenQueryPort).findAllByUserId(userId);

        // When
        blacklistService.blacklistAllUserTokens(userId);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        verify(globalJwtPort, never()).generateTokenHash(anyString());
        verify(redisJwtBlacklistPort, never()).blacklistTokenHashes(any(), any());
    }



    @Test
    @DisplayName("많은 수의 토큰을 블랙리스트에 등록")
    void shouldHandleManyTokens() {
        // Given
        Long userId = 100L;
        String reason = "Many tokens test";
        List<JwtToken> manyJwtTokens = createMultipleTokens(10);
        
        given(globalTokenQueryPort.findAllByUserId(userId)).willReturn(manyJwtTokens);
        for (int i = 0; i < 10; i++) {
            given(globalJwtPort.generateTokenHash("access-token-" + i)).willReturn("hash" + i);
        }

        // When
        blacklistService.blacklistAllUserTokens(userId);

        // Then
        verify(globalTokenQueryPort).findAllByUserId(userId);
        for (int i = 0; i < 10; i++) {
            verify(globalJwtPort).generateTokenHash("access-token-" + i);
        }

        ArgumentCaptor<List<String>> hashesCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisJwtBlacklistPort).blacklistTokenHashes(
                hashesCaptor.capture(),
                any(Duration.class)
        );
        
        List<String> capturedHashes = hashesCaptor.getValue();
        assertThat(capturedHashes).hasSize(10);
    }

    @Test
    @DisplayName("블랙리스트 추가 - 정상 케이스")
    void shouldAddToBlacklist_WhenValidParameters() {
        // Given
        Long userId = 1L;
        String socialId = "kakao12345";
        SocialProvider provider = SocialProvider.KAKAO;

        // When
        blacklistService.addToBlacklist(userId, socialId, provider);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(blacklistPort).saveBlackList(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList).isNotNull();
        assertThat(capturedBlackList.getSocialId()).isEqualTo(socialId);
        assertThat(capturedBlackList.getProvider()).isEqualTo(provider);
    }

    @Test
    @DisplayName("블랙리스트 추가 - 중복 등록 시 예외 처리")
    void shouldHandleException_WhenDuplicateEntry() {
        // Given
        Long userId = 1L;
        String socialId = "kakao12345";
        SocialProvider provider = SocialProvider.KAKAO;

        doThrow(new DataIntegrityViolationException("Duplicate entry"))
                .when(blacklistPort).saveBlackList(any(BlackList.class));

        // When - 예외가 발생해도 메서드가 정상 종료되어야 함 (로그만 출력)
        blacklistService.addToBlacklist(userId, socialId, provider);

        // Then
        verify(blacklistPort).saveBlackList(any(BlackList.class));
    }

    @Test
    @DisplayName("블랙리스트 추가 - null 파라미터 처리")
    void shouldCreateBlacklist_WithNullParameters() {
        // Given
        Long userId = null;
        String socialId = null;
        SocialProvider provider = null;

        // When
        blacklistService.addToBlacklist(userId, socialId, provider);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(blacklistPort).saveBlackList(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList).isNotNull();
        assertThat(capturedBlackList.getSocialId()).isNull();
        assertThat(capturedBlackList.getProvider()).isNull();
    }

    @Test
    @DisplayName("블랙리스트 추가 - 다양한 소셜 제공자")
    void shouldAddToBlacklist_WithVariousProviders() {
        // Given
        Long userId = 1L;
        String socialId = "socialUser123";
        SocialProvider provider = SocialProvider.KAKAO;

        // When
        blacklistService.addToBlacklist(userId, socialId, provider);

        // Then
        ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
        verify(blacklistPort).saveBlackList(blackListCaptor.capture());

        BlackList capturedBlackList = blackListCaptor.getValue();
        assertThat(capturedBlackList.getProvider()).isEqualTo(SocialProvider.KAKAO);
    }

    /**
     * 복수의 임시 토큰 생성 - BlacklistServiceTest 전용
     * 매 호출마다 새로운 리스트를 생성하여 반환
     */
    private List<JwtToken> createMultipleTokens(int count) {
        List<JwtToken> jwtTokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            jwtTokens.add(JwtToken.builder()
                    .accessToken("access-token-" + i)
                    .refreshToken("refresh-token-" + i)
                    .build());
        }
        return jwtTokens;
    }
}