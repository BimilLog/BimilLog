package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.infrastructure.config.AsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import jaeik.bimillog.infrastructure.redis.post.RedisDetailPostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>RealtimePopularScoreListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("RealtimePopularScoreListener 재시도 테스트")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {RealtimePopularScoreListener.class, RetryConfig.class, AsyncConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class RealtimePopularScoreListenerRetryTest {

    @Autowired
    private RealtimePopularScoreListener listener;

    @MockitoBean
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @MockitoBean
    private RedisDetailPostAdapter redisDetailPostAdapter;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(redisRealTimePostAdapter, redisDetailPostAdapter);
    }

    @Test
    @DisplayName("게시글 조회 이벤트 - RedisConnectionFailureException 발생 시 3회 재시도 후 @Recover 호출")
    void handlePostViewed_shouldRetryOnRedisConnectionFailure() {
        // Given
        PostViewedEvent event = new PostViewedEvent(1L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handlePostViewed(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(MAX_ATTEMPTS)).incrementRealtimePopularScore(1L, 2.0));
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleCommentCreated_shouldRetryOnRedisConnectionFailure() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handleCommentCreated(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(MAX_ATTEMPTS)).incrementRealtimePopularScore(100L, 3.0));
    }

    @Test
    @DisplayName("게시글 추천 이벤트 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handlePostLiked_shouldRetryOnRedisConnectionFailure() {
        // Given
        PostLikeEvent event = new PostLikeEvent(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(MAX_ATTEMPTS)).incrementRealtimePopularScore(1L, 4.0));
    }

    @Test
    @DisplayName("게시글 추천 취소 이벤트 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handlePostUnliked_shouldRetryOnRedisConnectionFailure() {
        // Given
        PostUnlikeEvent event = new PostUnlikeEvent(1L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handlePostUnliked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(MAX_ATTEMPTS)).incrementRealtimePopularScore(1L, -4.0));
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleCommentDeleted_shouldRetryOnRedisConnectionFailure() {
        // Given
        CommentDeletedEvent event = new CommentDeletedEvent(1L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handleCommentDeleted(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(MAX_ATTEMPTS)).incrementRealtimePopularScore(1L, -3.0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRetryScenarios")
    @DisplayName("Redis 예외 발생 시 재시도 후 성공")
    void shouldSucceedAfterRetry(String scenario, Runnable setupMock, Runnable executeListener, Runnable verifyMock) {
        // Given
        setupMock.run();

        // When
        executeListener.run();

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(verifyMock::run);
    }

    private Stream<Arguments> provideRetryScenarios() {
        return Stream.of(
                Arguments.of(
                        "게시글 조회 - 2회 실패 후 성공",
                        (Runnable) () -> willThrow(new RedisConnectionFailureException("실패"))
                                .willThrow(new RedisConnectionFailureException("실패"))
                                .willDoNothing()
                                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(1L, 2.0),
                        (Runnable) () -> listener.handlePostViewed(new PostViewedEvent(1L)),
                        (Runnable) () -> verify(redisRealTimePostAdapter, times(3)).incrementRealtimePopularScore(1L, 2.0)
                ),
                Arguments.of(
                        "댓글 작성 - 1회 실패 후 성공",
                        (Runnable) () -> willThrow(new RedisConnectionFailureException("실패"))
                                .willDoNothing()
                                .given(redisRealTimePostAdapter).incrementRealtimePopularScore(100L, 3.0),
                        (Runnable) () -> listener.handleCommentCreated(new CommentCreatedEvent(1L, "작성자", 2L, 100L)),
                        (Runnable) () -> verify(redisRealTimePostAdapter, times(2)).incrementRealtimePopularScore(100L, 3.0)
                )
        );
    }

    @Test
    @DisplayName("댓글 생성 이벤트 - 성공 시 실시간 인기글 점수 증가")
    void handleCommentCreated_shouldIncrementScoreOnSuccess() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);
        doNothing().when(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handleCommentCreated(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(100L, 3.0));
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 - 성공 시 실시간 인기글 점수 감소")
    void handleCommentDeleted_shouldDecrementScoreOnSuccess() {
        // Given
        CommentDeletedEvent event = new CommentDeletedEvent(100L);
        doNothing().when(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handleCommentDeleted(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(100L, -3.0));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 - 성공 시 실시간 인기글 점수 증가")
    void handlePostViewed_shouldIncrementScoreOnSuccess() {
        // Given
        PostViewedEvent event = new PostViewedEvent(1L);
        doNothing().when(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());

        // When
        listener.handlePostViewed(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(1L, 2.0));
    }

    @Test
    @DisplayName("게시글 추천 이벤트 - 성공 시 점수 증가 및 캐시 삭제")
    void handlePostLiked_shouldIncrementScoreAndDeleteCacheOnSuccess() {
        // Given
        PostLikeEvent event = new PostLikeEvent(1L, 2L, 3L);
        doNothing().when(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());
        doNothing().when(redisDetailPostAdapter).deleteCachePost(anyLong());

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(1L, 4.0);
                    verify(redisDetailPostAdapter, times(1)).deleteCachePost(1L);
                });
    }

    @Test
    @DisplayName("게시글 추천 취소 이벤트 - 성공 시 점수 감소 및 캐시 삭제")
    void handlePostUnliked_shouldDecrementScoreAndDeleteCacheOnSuccess() {
        // Given
        PostUnlikeEvent event = new PostUnlikeEvent(1L);
        doNothing().when(redisRealTimePostAdapter).incrementRealtimePopularScore(anyLong(), anyDouble());
        doNothing().when(redisDetailPostAdapter).deleteCachePost(anyLong());

        // When
        listener.handlePostUnliked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(1L, -4.0);
                    verify(redisDetailPostAdapter, times(1)).deleteCachePost(1L);
                });
    }
}
