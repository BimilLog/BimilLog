package jaeik.bimillog.domain.friend.listener;

import jaeik.bimillog.domain.friend.event.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendshipDeletedEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>FriendshipRedisListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("FriendshipRedisListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {FriendshipRedisListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class FriendshipRedisListenerRetryTest {

    @Autowired
    private FriendshipRedisListener listener;

    @MockitoBean
    private RedisFriendshipRepository redisFriendshipRepository;

    private static final int MAX_ATTEMPTS = 3;

    @Test
    @DisplayName("친구 관계 생성 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleFriendshipCreated_shouldRetryOnRedisConnectionFailure() {
        // Given
        FriendshipCreatedEvent event = new FriendshipCreatedEvent(1L, 2L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisFriendshipRepository).addFriend(anyLong(), anyLong());

        // When
        listener.handleFriendshipCreated(event);

        // Then
        verify(redisFriendshipRepository, times(MAX_ATTEMPTS))
                .addFriend(1L, 2L);
    }

    @Test
    @DisplayName("친구 관계 삭제 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleFriendshipDeleted_shouldRetryOnRedisConnectionFailure() {
        // Given
        FriendshipDeletedEvent event = new FriendshipDeletedEvent(1L, 2L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisFriendshipRepository).deleteFriend(anyLong(), anyLong());

        // When
        listener.handleFriendshipDeleted(event);

        // Then
        verify(redisFriendshipRepository, times(MAX_ATTEMPTS))
                .deleteFriend(1L, 2L);
    }

    @Test
    @DisplayName("친구 관계 생성 - 2회 실패 후 3회차에 성공")
    void handleFriendshipCreated_shouldSucceedAfterTwoFailures() {
        // Given
        FriendshipCreatedEvent event = new FriendshipCreatedEvent(1L, 2L);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new RedisConnectionFailureException("실패"))
                .willDoNothing()
                .given(redisFriendshipRepository).addFriend(1L, 2L);

        // When
        listener.handleFriendshipCreated(event);

        // Then
        verify(redisFriendshipRepository, times(3))
                .addFriend(1L, 2L);
    }

    @Test
    @DisplayName("친구 관계 삭제 - 1회 성공 시 재시도 없음")
    void handleFriendshipDeleted_shouldNotRetryOnSuccess() {
        // Given
        FriendshipDeletedEvent event = new FriendshipDeletedEvent(1L, 2L);
        doNothing().when(redisFriendshipRepository).deleteFriend(anyLong(), anyLong());

        // When
        listener.handleFriendshipDeleted(event);

        // Then
        verify(redisFriendshipRepository, times(1))
                .deleteFriend(1L, 2L);
    }
}
