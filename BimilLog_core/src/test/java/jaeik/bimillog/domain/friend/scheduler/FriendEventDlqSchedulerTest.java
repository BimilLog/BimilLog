package jaeik.bimillog.domain.friend.scheduler;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.domain.friend.repository.FriendEventDlqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.HashOperations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FriendEventDlqScheduler 테스트</h2>
 * <p>DLQ 스케줄러의 재처리 로직을 검증합니다.</p>
 */
@DisplayName("FriendEventDlqScheduler 테스트")
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class FriendEventDlqSchedulerTest {

    @InjectMocks
    private FriendEventDlqScheduler scheduler;

    @Mock
    private FriendEventDlqRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HealthEndpoint healthEndpoint;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("Redis 비정상 상태면 재처리 건너뜀")
    void processDlq_shouldSkipWhenRedisUnhealthy() {
        // Given
        Health unhealthyRedis = Health.down().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", unhealthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        // When
        scheduler.processDlq();

        // Then
        verify(repository, never()).findPendingEvents(anyInt());
    }

    @Test
    @DisplayName("PENDING 이벤트가 없으면 처리 없음")
    void processDlq_shouldDoNothingWhenNoEvents() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);
        given(repository.findPendingEvents(anyInt())).willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(redisTemplate, never()).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("FRIEND_ADD 이벤트 재처리 성공")
    void processDlq_shouldProcessFriendAddEvent() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        FriendEventDlq event = FriendEventDlq.createFriendAdd(1L, 2L);
        given(repository.findPendingEvents(anyInt())).willReturn(List.of(event));
        given(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(redisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(event.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PROCESSED);
    }

    @Test
    @DisplayName("FRIEND_REMOVE 이벤트 재처리 성공")
    void processDlq_shouldProcessFriendRemoveEvent() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        FriendEventDlq event = FriendEventDlq.createFriendRemove(1L, 2L);
        given(repository.findPendingEvents(anyInt())).willReturn(List.of(event));
        given(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(redisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(event.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PROCESSED);
    }

    @Test
    @DisplayName("SCORE_UP 이벤트 재처리 성공")
    void processDlq_shouldProcessScoreUpEvent() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        FriendEventDlq event = FriendEventDlq.createScoreUp("test-score-event-id", 1L, 2L, 0.5);
        given(repository.findPendingEvents(anyInt())).willReturn(List.of(event));
        given(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(redisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(event.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PROCESSED);
    }

    @Test
    @DisplayName("파이프라인 실패 시 개별 재처리 후 retryCount 증가")
    void processDlq_shouldIncrementRetryCountOnFailure() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        FriendEventDlq event = FriendEventDlq.createFriendAdd(1L, 2L);
        given(repository.findPendingEvents(anyInt())).willReturn(List.of(event));
        given(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willThrow(new RedisConnectionFailureException("파이프라인 실패"));
        doThrow(new RedisConnectionFailureException("개별 처리 실패"))
                .when(setOperations).add(anyString(), any());

        // When
        scheduler.processDlq();

        // Then
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PENDING);
    }

    @Test
    @DisplayName("최대 재시도 초과 시 FAILED 상태로 변경")
    void processDlq_shouldMarkAsFailedAfterMaxRetries() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        FriendEventDlq event = FriendEventDlq.createFriendAdd(1L, 2L);
        // 이미 2회 재시도한 상태 시뮬레이션
        event.incrementRetryCount();
        event.incrementRetryCount();

        given(repository.findPendingEvents(anyInt())).willReturn(List.of(event));
        given(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willThrow(new RedisConnectionFailureException("파이프라인 실패"));
        doThrow(new RedisConnectionFailureException("개별 처리 실패"))
                .when(setOperations).add(anyString(), any());

        // When
        scheduler.processDlq();

        // Then
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.FAILED);
    }

    @Test
    @DisplayName("여러 이벤트 동시 처리")
    void processDlq_shouldProcessMultipleEvents() {
        // Given
        Health healthyRedis = Health.up().build();
        Health compositeHealth = Health.up()
                .withDetail("redis", healthyRedis)
                .build();
        given(healthEndpoint.health()).willReturn(compositeHealth);

        FriendEventDlq addEvent = FriendEventDlq.createFriendAdd(1L, 2L);
        FriendEventDlq removeEvent = FriendEventDlq.createFriendRemove(3L, 4L);
        FriendEventDlq scoreEvent = FriendEventDlq.createScoreUp("test-score-event-id-2", 5L, 6L, 0.5);

        given(repository.findPendingEvents(anyInt())).willReturn(List.of(addEvent, removeEvent, scoreEvent));
        given(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(redisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(addEvent.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PROCESSED);
        assertThat(removeEvent.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PROCESSED);
        assertThat(scoreEvent.getStatus()).isEqualTo(FriendEventDlq.DlqStatus.PROCESSED);
    }
}
