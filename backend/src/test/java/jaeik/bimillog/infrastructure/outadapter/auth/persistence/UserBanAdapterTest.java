package jaeik.bimillog.infrastructure.outadapter.auth.persistence;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.auth.out.persistence.UserBanAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.blacklist.BlackListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>UserBanAdapter 단위 테스트</h2>
 * <p>사용자 차단 어댑터의 핵심 기능 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserBanAdapterTest {

    private static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;
    private static final String TEST_SOCIAL_ID = "123456789";
    private static final String TEST_TOKEN_HASH = "abc123def456";
    private static final String TEST_REASON = "User logout";
    private static final Duration TEST_TTL = Duration.ofHours(24);

    @Mock private BlackListRepository blackListRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private UserBanAdapter userBanAdapter;

    @Test
    @DisplayName("블랙리스트 존재 확인 - 존재하는 사용자")
    void shouldReturnTrue_WhenUserExistsInBlacklist() {
        // Given
        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(true);

        // When
        boolean result = userBanAdapter.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);

        // Then
        assertThat(result).isTrue();
        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 존재하지 않는 사용자")
    void shouldReturnFalse_WhenUserNotExistsInBlacklist() {
        // Given
        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);

        // When
        boolean result = userBanAdapter.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);

        // Then
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
    }

    @Test
    @DisplayName("예외 전파 테스트 - Repository에서 예외 발생")
    void shouldPropagateException_WhenRepositoryThrowsException() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database connection failed");
        given(blackListRepository.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> userBanAdapter.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(blackListRepository).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - 존재하는 토큰 해시")
    void shouldReturnTrue_WhenTokenHashExistsInBlacklist() {
        // Given
        String expectedKey = "token:blacklist:" + TEST_TOKEN_HASH;
        given(redisTemplate.hasKey(expectedKey)).willReturn(true);

        // When
        boolean result = userBanAdapter.isBlacklisted(TEST_TOKEN_HASH);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - 존재하지 않는 토큰 해시")
    void shouldReturnFalse_WhenTokenHashNotInBlacklist() {
        // Given
        String expectedKey = "token:blacklist:" + TEST_TOKEN_HASH;
        given(redisTemplate.hasKey(expectedKey)).willReturn(false);

        // When
        boolean result = userBanAdapter.isBlacklisted(TEST_TOKEN_HASH);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Redis 장애 시 안전한 처리")
    void shouldReturnTrue_WhenRedisFailureOccurs() {
        // Given
        String expectedKey = "token:blacklist:" + TEST_TOKEN_HASH;
        given(redisTemplate.hasKey(expectedKey))
                .willThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = userBanAdapter.isBlacklisted(TEST_TOKEN_HASH);

        // Then
        assertThat(result).isTrue(); // 보안상 안전한 기본값
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("토큰 해시 블랙리스트 등록")
    void shouldBlacklistTokenHashes_WhenValidHashesProvided() {
        // Given
        List<String> tokenHashes = Arrays.asList("hash1", "hash2", "hash3");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When
        userBanAdapter.blacklistTokenHashes(tokenHashes, TEST_REASON, TEST_TTL);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations, times(3)).set(keyCaptor.capture(), any(), ttlCaptor.capture());

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertThat(capturedKeys).containsExactly(
                "token:blacklist:hash1",
                "token:blacklist:hash2",
                "token:blacklist:hash3"
        );

        List<Duration> capturedTtls = ttlCaptor.getAllValues();
        assertThat(capturedTtls).allSatisfy(capturedTtl ->
                assertThat(capturedTtl).isEqualTo(TEST_TTL)
        );
    }

    @Test
    @DisplayName("빈 리스트 처리")
    void shouldHandleEmptyList_WhenNoTokenHashesProvided() {
        // Given
        List<String> emptyTokenHashes = Collections.emptyList();

        // When
        userBanAdapter.blacklistTokenHashes(emptyTokenHashes, TEST_REASON, TEST_TTL);

        // Then
        verify(redisTemplate, never()).opsForValue();
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("null 리스트 처리")
    void shouldHandleNullList_WhenNullTokenHashesProvided() {
        // Given
        List<String> nullTokenHashes = null;

        // When
        userBanAdapter.blacklistTokenHashes(nullTokenHashes, TEST_REASON, TEST_TTL);

        // Then
        verify(redisTemplate, never()).opsForValue();
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("Redis 저장 실패 시 예외 처리")
    void shouldThrowException_WhenRedisOperationFails() {
        // Given
        List<String> tokenHashes = Arrays.asList("failHash123");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RuntimeException("Redis write operation failed"))
                .given(valueOperations).set(anyString(), any(), any(Duration.class));

        // When & Then
        assertThatThrownBy(() ->
                userBanAdapter.blacklistTokenHashes(tokenHashes, TEST_REASON, TEST_TTL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis token blacklist operation failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}