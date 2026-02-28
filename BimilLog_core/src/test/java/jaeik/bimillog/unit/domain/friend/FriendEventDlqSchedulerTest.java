package jaeik.bimillog.unit.domain.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendDlqStatus;
import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.domain.friend.repository.FriendEventDlqRepository;
import jaeik.bimillog.domain.friend.scheduler.FriendEventDlqScheduler;
import jaeik.bimillog.infrastructure.redis.RedisCheck;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisCheck redisCheck;

    @Mock
    private RedisFriendRestore redisFriendRestore;

    @Mock
    private RedisFriendshipRepository redisFriendshipRepository;

    @Mock
    private RedisInteractionScoreRepository redisInteractionScoreRepository;

    @BeforeEach
    void setUp() {
        // 기본적으로 Redis 정상 상태
        lenient().when(redisCheck.isRedisHealthy()).thenReturn(true);
    }

    @Test
    @DisplayName("Redis 비정상 상태면 재처리 건너뜀")
    void processDlq_shouldSkipWhenRedisUnhealthy() {
        // Given
        given(redisCheck.isRedisHealthy()).willReturn(false);

        // When
        scheduler.processDlq();

        // Then - 이벤트 조회도 안함
        verify(repository, never()).findPendingEvents(any(), anyInt(), anyInt());
        verify(stringRedisTemplate, never()).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    @Test
    @DisplayName("PENDING 이벤트가 없으면 처리 없음")
    void processDlq_shouldDoNothingWhenNoEvents() {
        // Given
        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then - 파이프라인 실행 안함
        verify(stringRedisTemplate, never()).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    @Test
    @DisplayName("FRIEND_ADD 이벤트 재처리 성공")
    void processDlq_shouldProcessFriendAddEvent() {
        // Given
        FriendEventDlq event = FriendEventDlq.createFriendAdd(1L, 2L);
        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(List.of(event))
                .willReturn(Collections.emptyList());
        given(stringRedisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(stringRedisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(event.getStatus()).isEqualTo(FriendDlqStatus.PROCESSED);
    }

    @Test
    @DisplayName("FRIEND_REMOVE 이벤트 재처리 성공")
    void processDlq_shouldProcessFriendRemoveEvent() {
        // Given
        FriendEventDlq event = FriendEventDlq.createFriendRemove(1L, 2L);
        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(List.of(event))
                .willReturn(Collections.emptyList());
        given(stringRedisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(stringRedisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(event.getStatus()).isEqualTo(FriendDlqStatus.PROCESSED);
    }

    @Test
    @DisplayName("SCORE_UP 이벤트 재처리 성공")
    void processDlq_shouldProcessScoreUpEvent() {
        // Given
        FriendEventDlq event = FriendEventDlq.createScoreUp("test-score-event-id", 1L, 2L, 0.5);
        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(List.of(event))
                .willReturn(Collections.emptyList());
        given(stringRedisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(stringRedisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(event.getStatus()).isEqualTo(FriendDlqStatus.PROCESSED);
    }

    @Test
    @DisplayName("파이프라인 실패 시 개별 재처리 후 retryCount 증가")
    @SuppressWarnings("unchecked")
    void processDlq_shouldIncrementRetryCountOnFailure() {
        // Given
        FriendEventDlq event = FriendEventDlq.createFriendAdd(1L, 2L);
        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(List.of(event))
                .willReturn(Collections.emptyList());
        given(stringRedisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willThrow(new RedisConnectionFailureException("파이프라인 실패"));

        // 개별 처리도 실패하도록 설정
        doThrow(new RedisConnectionFailureException("개별 처리 실패"))
                .when(redisFriendshipRepository).addFriend(anyLong(), anyLong());

        // When
        scheduler.processDlq();

        // Then
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(FriendDlqStatus.PENDING);
    }

    @Test
    @DisplayName("최대 재시도 초과 시 FAILED 상태로 변경")
    @SuppressWarnings("unchecked")
    void processDlq_shouldMarkAsFailedAfterMaxRetries() {
        // Given
        FriendEventDlq event = FriendEventDlq.createFriendAdd(1L, 2L);
        // 이미 2회 재시도한 상태 시뮬레이션
        event.incrementRetryCount();
        event.incrementRetryCount();

        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(List.of(event))
                .willReturn(Collections.emptyList());
        given(stringRedisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willThrow(new RedisConnectionFailureException("파이프라인 실패"));

        // 개별 처리도 실패하도록 설정
        doThrow(new RedisConnectionFailureException("개별 처리 실패"))
                .when(redisFriendshipRepository).addFriend(anyLong(), anyLong());

        // When
        scheduler.processDlq();

        // Then
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(FriendDlqStatus.FAILED);
    }

    @Test
    @DisplayName("여러 이벤트 동시 처리")
    void processDlq_shouldProcessMultipleEvents() {
        // Given
        FriendEventDlq addEvent = FriendEventDlq.createFriendAdd(1L, 2L);
        FriendEventDlq removeEvent = FriendEventDlq.createFriendRemove(3L, 4L);
        FriendEventDlq scoreEvent = FriendEventDlq.createScoreUp("test-score-event-id-2", 5L, 6L, 0.5);

        given(repository.findPendingEvents(eq(FriendDlqStatus.PENDING), eq(3), anyInt()))
                .willReturn(List.of(addEvent, removeEvent, scoreEvent))
                .willReturn(Collections.emptyList());
        given(stringRedisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .willReturn(Collections.emptyList());

        // When
        scheduler.processDlq();

        // Then
        verify(stringRedisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(repository, atLeast(1)).saveAll(anyList());
        assertThat(addEvent.getStatus()).isEqualTo(FriendDlqStatus.PROCESSED);
        assertThat(removeEvent.getStatus()).isEqualTo(FriendDlqStatus.PROCESSED);
        assertThat(scoreEvent.getStatus()).isEqualTo(FriendDlqStatus.PROCESSED);
    }
}
