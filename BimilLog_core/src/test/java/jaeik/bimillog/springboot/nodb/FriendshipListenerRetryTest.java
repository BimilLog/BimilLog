package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.friend.listener.FriendshipListener;
import jaeik.bimillog.domain.friend.event.FriendEvent.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent.FriendshipDeletedEvent;
import jaeik.bimillog.domain.friend.rebuild.FriendEventDlqService;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>FriendshipListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("FriendshipListener 재시도 테스트")
@SpringBootTest(classes = {
        FriendshipListener.class,
        FriendshipListenerRetryTest.TestConfig.class
})
@Tag("springboot-nodb")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FriendshipListenerRetryTest {

    @TestConfiguration
    @EnableAsync
    @EnableRetry
    static class TestConfig {
        @Bean(name = "friendUpdateExecutor")
        @Primary
        public Executor friendUpdateExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(5);
            executor.setQueueCapacity(10);
            executor.setThreadNamePrefix("test-friend-update-");
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(10);
            executor.initialize();
            return executor;
        }
    }

    @Autowired
    private FriendshipListener friendshipListener;

    @MockitoBean
    private RedisFriendshipRepository redisFriendshipRepository;

    @MockitoBean
    private FriendEventDlqService friendEventDlqService;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(redisFriendshipRepository, friendEventDlqService);
        Mockito.clearInvocations(redisFriendshipRepository, friendEventDlqService);
    }

    @Test
    @DisplayName("친구 관계 추가 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleFriendshipCreated_shouldRetryOnRedisConnectionFailure() {
        // Given
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisFriendshipRepository).addFriend(anyLong(), anyLong());

        // When
        friendshipListener.handleFriendshipCreated(new FriendshipCreatedEvent(1L, 2L));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisFriendshipRepository, times(MAX_ATTEMPTS))
                        .addFriend(1L, 2L));
    }

    @Test
    @DisplayName("친구 관계 추가 - 3회 재시도 실패 후 DLQ 저장")
    void handleFriendshipCreated_shouldSaveToDlqAfterMaxRetries() {
        // Given
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisFriendshipRepository).addFriend(anyLong(), anyLong());

        // When
        friendshipListener.handleFriendshipCreated(new FriendshipCreatedEvent(1L, 2L));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(friendEventDlqService, times(1))
                        .saveFriendAdd(eq(1L), eq(2L)));
    }

    @Test
    @DisplayName("친구 관계 삭제 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleFriendshipDeleted_shouldRetryOnRedisConnectionFailure() {
        // Given
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisFriendshipRepository).deleteFriend(anyLong(), anyLong());

        // When
        friendshipListener.handleFriendshipDeleted(new FriendshipDeletedEvent(1L, 2L));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisFriendshipRepository, times(MAX_ATTEMPTS))
                        .deleteFriend(1L, 2L));
    }

    @Test
    @DisplayName("친구 관계 삭제 - 3회 재시도 실패 후 DLQ 저장")
    void handleFriendshipDeleted_shouldSaveToDlqAfterMaxRetries() {
        // Given
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisFriendshipRepository).deleteFriend(anyLong(), anyLong());

        // When
        friendshipListener.handleFriendshipDeleted(new FriendshipDeletedEvent(1L, 2L));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(friendEventDlqService, times(1))
                        .saveFriendRemove(eq(1L), eq(2L)));
    }

    @Test
    @DisplayName("친구 관계 추가 - 2회 실패 후 3회차에 성공 - DLQ 저장 안함")
    void handleFriendshipCreated_shouldSucceedAfterTwoFailures_noDlqSave() {
        // Given
        doThrow(new RedisConnectionFailureException("실패"))
                .doThrow(new RedisConnectionFailureException("실패"))
                .doNothing()
                .when(redisFriendshipRepository).addFriend(1L, 2L);

        // When
        friendshipListener.handleFriendshipCreated(new FriendshipCreatedEvent(1L, 2L));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisFriendshipRepository, times(3)).addFriend(1L, 2L);
                    verify(friendEventDlqService, never()).saveFriendAdd(anyLong(), anyLong());
                });
    }

    @Test
    @DisplayName("친구 관계 삭제 - 1회 성공 시 재시도 및 DLQ 저장 없음")
    void handleFriendshipDeleted_shouldNotRetryOnSuccess() {
        // Given
        doNothing().when(redisFriendshipRepository).deleteFriend(anyLong(), anyLong());

        // When
        friendshipListener.handleFriendshipDeleted(new FriendshipDeletedEvent(1L, 2L));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisFriendshipRepository, times(1)).deleteFriend(1L, 2L);
                    verify(friendEventDlqService, never()).saveFriendRemove(anyLong(), anyLong());
                });
    }
}
