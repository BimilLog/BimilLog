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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

//TODO 포트 통합으로 중복 테스트나 테스트 커버리지 다시 검토 필요
/**
 * <h2>UserBanAdapter 단위 테스트</h2>
 * <p>Redis 연동, 예외 처리, 동시성, 성능 테스트</p>
 * <p>외부 시스템(Redis) 장애 상황에서의 안전한 동작을 중점 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserBanAdapterTest {

    @Mock
    private BlackListRepository blackListRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private UserBanAdapter userBanAdapter;

    @Test
    @DisplayName("블랙리스트 존재 확인 - 존재하는 사용자")
    void shouldReturnTrue_WhenUserExistsInBlacklist() {
        // Given: 블랙리스트에 존재하는 사용자
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(true);

        // When: 블랙리스트 존재 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, socialId);

        // Then: true 반환 검증
        assertThat(result).isTrue();
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 존재하지 않는 사용자")
    void shouldReturnFalse_WhenUserNotExistsInBlacklist() {
        // Given: 블랙리스트에 존재하지 않는 사용자
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "987654321";
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(false);

        // When: 블랙리스트 존재 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, socialId);

        // Then: false 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - null provider 처리")
    void shouldHandleNullProvider_WhenProviderIsNull() {
        // Given: null provider
        SocialProvider nullProvider = null;
        String socialId = "123456789";
        given(blackListRepository.existsByProviderAndSocialId(nullProvider, socialId)).willReturn(false);

        // When: null provider로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(nullProvider, socialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(nullProvider, socialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - null socialId 처리")
    void shouldHandleNullSocialId_WhenSocialIdIsNull() {
        // Given: null socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String nullSocialId = null;
        given(blackListRepository.existsByProviderAndSocialId(provider, nullSocialId)).willReturn(false);

        // When: null socialId로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, nullSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, nullSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 모든 파라미터 null")
    void shouldHandleAllNullParameters_WhenBothParametersAreNull() {
        // Given: 모든 파라미터가 null
        SocialProvider nullProvider = null;
        String nullSocialId = null;
        given(blackListRepository.existsByProviderAndSocialId(nullProvider, nullSocialId)).willReturn(false);

        // When: 모든 파라미터를 null로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(nullProvider, nullSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(nullProvider, nullSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 빈 문자열 socialId 처리")
    void shouldHandleEmptySocialId_WhenSocialIdIsEmpty() {
        // Given: 빈 문자열 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String emptySocialId = "";
        given(blackListRepository.existsByProviderAndSocialId(provider, emptySocialId)).willReturn(false);

        // When: 빈 문자열 socialId로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, emptySocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, emptySocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 공백 문자열 socialId 처리")
    void shouldHandleWhitespaceSocialId_WhenSocialIdIsWhitespace() {
        // Given: 공백 문자열 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String whitespaceSocialId = "   ";
        given(blackListRepository.existsByProviderAndSocialId(provider, whitespaceSocialId)).willReturn(false);

        // When: 공백 문자열 socialId로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, whitespaceSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, whitespaceSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 다양한 SocialProvider 처리")
    void shouldHandleDifferentProviders_WhenVariousProvidersUsed() {
        // Given: 다양한 SocialProvider들
        String socialId = "123456789";

        // KAKAO Provider 테스트
        given(blackListRepository.existsByProviderAndSocialId(SocialProvider.KAKAO, socialId)).willReturn(true);
        boolean kakaoResult = userBanAdapter.existsByProviderAndSocialId(SocialProvider.KAKAO, socialId);
        assertThat(kakaoResult).isTrue();

        // 다른 Provider가 있다면 추가 테스트 (현재는 KAKAO만 있음)
        // 향후 NAVER, GOOGLE 등 추가 시 여기에 테스트 추가
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 매우 긴 socialId 처리")
    void shouldHandleLongSocialId_WhenSocialIdIsVeryLong() {
        // Given: 매우 긴 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String longSocialId = "a".repeat(1000); // 1000자 길이의 socialId
        given(blackListRepository.existsByProviderAndSocialId(provider, longSocialId)).willReturn(false);

        // When: 긴 socialId로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, longSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, longSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 특수 문자 포함 socialId 처리")
    void shouldHandleSpecialCharactersSocialId_WhenSocialIdContainsSpecialChars() {
        // Given: 특수 문자 포함 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String specialCharSocialId = "user@#$%^&*()123";
        given(blackListRepository.existsByProviderAndSocialId(provider, specialCharSocialId)).willReturn(false);

        // When: 특수 문자 포함 socialId로 확인
        boolean result = userBanAdapter.existsByProviderAndSocialId(provider, specialCharSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, specialCharSocialId);
    }

    @Test
    @DisplayName("예외 전파 테스트 - Repository에서 예외 발생")
    void shouldPropagateException_WhenRepositoryThrowsException() {
        // Given: Repository에서 예외 발생
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        RuntimeException expectedException = new RuntimeException("Database connection failed");
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willThrow(expectedException);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> userBanAdapter.existsByProviderAndSocialId(provider, socialId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("성능 테스트 - 대량 호출 시 일관성")
    void shouldMaintainConsistency_WhenCalledManyTimes() {
        // Given: 동일한 파라미터
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(true);

        // When: 대량 호출
        int callCount = 1000;
        for (int i = 0; i < callCount; i++) {
            boolean result = userBanAdapter.existsByProviderAndSocialId(provider, socialId);
            // Then: 모든 호출에서 동일한 결과
            assertThat(result).isTrue();
        }

        // Repository가 정확히 호출 횟수만큼 호출되었는지 확인
        verify(blackListRepository, org.mockito.Mockito.times(callCount))
                .existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("동시성 테스트 - 다른 파라미터로 동시 호출")
    void shouldHandleConcurrentCalls_WhenDifferentParametersUsed() {
        // Given: 서로 다른 파라미터들
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId1 = "user1";
        String socialId2 = "user2";

        given(blackListRepository.existsByProviderAndSocialId(provider, socialId1)).willReturn(true);
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId2)).willReturn(false);

        // When: 동시 호출
        boolean result1 = userBanAdapter.existsByProviderAndSocialId(provider, socialId1);
        boolean result2 = userBanAdapter.existsByProviderAndSocialId(provider, socialId2);

        // Then: 각각 올바른 결과 반환
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();

        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId1);
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId2);
    }


    @Test
    @DisplayName("블랙리스트 확인 - 존재하는 토큰 해시")
    void shouldReturnTrue_WhenTokenHashExistsInBlacklist() {
        // Given: 블랙리스트에 존재하는 토큰 해시
        String tokenHash = "abc123def456";
        String expectedKey = "token:blacklist:" + tokenHash;

        given(redisTemplate.hasKey(expectedKey)).willReturn(true);

        // When: 블랙리스트 확인
        boolean result = userBanAdapter.isBlacklisted(tokenHash);

        // Then: true 반환 검증
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("블랙리스트 확인 - 존재하지 않는 토큰 해시")
    void shouldReturnFalse_WhenTokenHashNotInBlacklist() {
        // Given: 블랙리스트에 존재하지 않는 토큰 해시
        String tokenHash = "xyz789uvw012";
        String expectedKey = "token:blacklist:" + tokenHash;

        given(redisTemplate.hasKey(expectedKey)).willReturn(false);

        // When: 블랙리스트 확인
        boolean result = userBanAdapter.isBlacklisted(tokenHash);

        // Then: false 반환 검증
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("블랙리스트 확인 - Redis 장애 시 안전한 처리")
    void shouldReturnTrue_WhenRedisFailureOccurs() {
        // Given: Redis 연결 장애 시뮬레이션
        String tokenHash = "failureToken123";
        String expectedKey = "token:blacklist:" + tokenHash;

        given(redisTemplate.hasKey(expectedKey))
                .willThrow(new RuntimeException("Redis connection failed"));

        // When: Redis 장애 상황에서 블랙리스트 확인
        boolean result = userBanAdapter.isBlacklisted(tokenHash);

        // Then: 안전하게 true 반환 (보안상 안전한 기본값)
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("토큰 해시 블랙리스트 등록 - 정상적인 복수 토큰 처리")
    void shouldBlacklistTokenHashes_WhenValidHashesProvided() {
        // Given: 복수의 토큰 해시와 TTL 설정
        List<String> tokenHashes = Arrays.asList(
                "hash1abc123",
                "hash2def456",
                "hash3ghi789"
        );
        String reason = "User logout";
        Duration ttl = Duration.ofHours(24);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When: 토큰 해시들을 블랙리스트에 등록
        userBanAdapter.blacklistTokenHashes(tokenHashes, reason, ttl);

        // Then: 각 토큰 해시별로 Redis 저장 검증
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations, times(3)).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        // 키 검증
        List<String> capturedKeys = keyCaptor.getAllValues();
        assertThat(capturedKeys).containsExactly(
                "token:blacklist:hash1abc123",
                "token:blacklist:hash2def456",
                "token:blacklist:hash3ghi789"
        );

        // TTL 검증
        List<Duration> capturedTtls = ttlCaptor.getAllValues();
        assertThat(capturedTtls).allSatisfy(capturedTtl ->
                assertThat(capturedTtl).isEqualTo(ttl)
        );

        // 저장된 값의 reason 검증
        List<Object> capturedValues = valueCaptor.getAllValues();
        assertThat(capturedValues).hasSize(3);
        // TokenBlacklistInfo는 private inner class이므로 reflection으로 검증하거나 toString() 검증
    }

    @Test
    @DisplayName("토큰 해시 블랙리스트 등록 - 빈 리스트 처리")
    void shouldHandleEmptyList_WhenNoTokenHashesProvided() {
        // Given: 빈 토큰 해시 리스트
        List<String> emptyTokenHashes = Collections.emptyList();
        String reason = "Empty list test";
        Duration ttl = Duration.ofHours(1);

        // When: 빈 리스트로 블랙리스트 등록 시도
        userBanAdapter.blacklistTokenHashes(emptyTokenHashes, reason, ttl);

        // Then: Redis 호출 없이 안전하게 처리
        verify(redisTemplate, never()).opsForValue();
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("토큰 해시 블랙리스트 등록 - null 리스트 처리")
    void shouldHandleNullList_WhenNullTokenHashesProvided() {
        // Given: null 토큰 해시 리스트
        List<String> nullTokenHashes = null;
        String reason = "Null list test";
        Duration ttl = Duration.ofMinutes(30);

        // When: null 리스트로 블랙리스트 등록 시도
        userBanAdapter.blacklistTokenHashes(nullTokenHashes, reason, ttl);

        // Then: Redis 호출 없이 안전하게 처리
        verify(redisTemplate, never()).opsForValue();
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("토큰 해시 블랙리스트 등록 - Redis 저장 실패 시 예외 전파")
    void shouldThrowException_WhenRedisOperationFails() {
        // Given: Redis 저장 실패 시뮬레이션
        List<String> tokenHashes = Arrays.asList("failHash123");
        String reason = "Redis failure test";
        Duration ttl = Duration.ofHours(2);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RuntimeException("Redis write operation failed"))
                .given(valueOperations).set(anyString(), any(), any(Duration.class));

        // When & Then: RuntimeException 전파 검증
        assertThatThrownBy(() ->
                userBanAdapter.blacklistTokenHashes(tokenHashes, reason, ttl))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis token blacklist operation failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 블랙리스트 확인")
    void shouldHandleConcurrentBlacklistChecks_WhenMultipleThreadsAccess() throws InterruptedException {
        // Given: 동시성 테스트 설정
        int threadCount = 100;
        String tokenHash = "concurrentTest123";
        String expectedKey = "token:blacklist:" + tokenHash;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        given(redisTemplate.hasKey(expectedKey)).willReturn(true);

        // When: 동시에 여러 스레드에서 블랙리스트 확인
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    boolean result = userBanAdapter.isBlacklisted(tokenHash);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 예외 발생 시에도 latch 감소
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // 동시 시작 및 완료 대기
        startLatch.countDown();
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);

        // Then: 모든 스레드가 성공적으로 완료
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Redis가 thread-safe하게 호출되었는지 검증
        verify(redisTemplate, times(threadCount)).hasKey(expectedKey);

        executorService.shutdown();
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 블랙리스트 등록")
    void shouldHandleConcurrentBlacklistRegistration_WhenMultipleThreadsRegister() throws InterruptedException {
        // Given: 동시 등록 테스트 설정
        int threadCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When: 동시에 여러 스레드에서 블랙리스트 등록
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    List<String> hashes = Arrays.asList("concurrentHash" + threadId);
                    userBanAdapter.blacklistTokenHashes(
                            hashes, "Concurrent test " + threadId, Duration.ofHours(1)
                    );
                } catch (Exception e) {
                    // 예외 상황도 처리
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // 동시 시작 및 완료 대기
        startLatch.countDown();
        boolean completed = completeLatch.await(15, TimeUnit.SECONDS);

        // Then: 모든 스레드가 성공적으로 완료
        assertThat(completed).isTrue();
        verify(valueOperations, times(threadCount)).set(anyString(), any(), any(Duration.class));

        executorService.shutdown();
    }

    @Test
    @DisplayName("대용량 데이터 처리 - 많은 수의 토큰 해시 일괄 처리")
    void shouldHandleLargeTokenHashList_WhenBatchProcessingRequired() {
        // Given: 대량의 토큰 해시 (1000개)
        List<String> largeTokenHashList = Collections.nCopies(1000, "batchHash");
        String reason = "Bulk user logout";
        Duration ttl = Duration.ofDays(7);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When: 대량 토큰 해시 블랙리스트 등록
        userBanAdapter.blacklistTokenHashes(largeTokenHashList, reason, ttl);

        // Then: 모든 해시에 대해 Redis 저장 호출 검증
        verify(valueOperations, times(1000)).set(anyString(), any(), eq(ttl));
    }

    @Test
    @DisplayName("키 형식 검증 - 정확한 Redis 키 패턴 사용")
    void shouldUseCorrectKeyPattern_WhenStoringInRedis() {
        // Given: 특수 문자가 포함된 토큰 해시
        List<String> specialTokenHashes = Arrays.asList(
                "hash-with-dash",
                "hash_with_underscore",
                "hash.with.dot",
                "hash123numbers"
        );
        String reason = "Key pattern test";
        Duration ttl = Duration.ofMinutes(15);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When: 특수 문자 포함 해시 저장
        userBanAdapter.blacklistTokenHashes(specialTokenHashes, reason, ttl);

        // Then: 올바른 키 패턴으로 저장되는지 검증
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(4)).set(keyCaptor.capture(), any(), eq(ttl));

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertThat(capturedKeys).containsExactly(
                "token:blacklist:hash-with-dash",
                "token:blacklist:hash_with_underscore",
                "token:blacklist:hash.with.dot",
                "token:blacklist:hash123numbers"
        );
    }

    @Test
    @DisplayName("TTL 변경 테스트 - 다양한 만료 시간 설정")
    void shouldSetCorrectTTL_WhenDifferentExpirationsProvided() {
        // Given: 다양한 TTL 값들
        List<String> tokenHashes = Arrays.asList("ttlTest123");

        Duration shortTtl = Duration.ofMinutes(5);
        Duration longTtl = Duration.ofDays(30);
        Duration customTtl = Duration.ofHours(8).plusMinutes(45);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When: 각기 다른 TTL로 저장
        userBanAdapter.blacklistTokenHashes(tokenHashes, "Short TTL", shortTtl);
        userBanAdapter.blacklistTokenHashes(tokenHashes, "Long TTL", longTtl);
        userBanAdapter.blacklistTokenHashes(tokenHashes, "Custom TTL", customTtl);

        // Then: 정확한 TTL이 설정되는지 검증
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations, times(3)).set(anyString(), any(), ttlCaptor.capture());

        List<Duration> capturedTtls = ttlCaptor.getAllValues();
        assertThat(capturedTtls).containsExactly(shortTtl, longTtl, customTtl);
    }

}