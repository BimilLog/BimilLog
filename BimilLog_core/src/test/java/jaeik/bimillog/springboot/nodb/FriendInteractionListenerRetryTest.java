package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentLikeEvent;
import jaeik.bimillog.domain.friend.listener.FriendInteractionListener;
import jaeik.bimillog.domain.friend.service.FriendEventDlqService;
import jaeik.bimillog.domain.post.event.PostLikedEvent;
import jaeik.bimillog.infrastructure.config.async.AsyncConfig;
import jaeik.bimillog.infrastructure.config.async.FriendAsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static jaeik.bimillog.domain.friend.listener.FriendInteractionListener.INTERACTION_SCORE_DEFAULT;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>FriendInteractionListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("FriendInteractionListener 재시도 테스트")
@SpringBootTest(classes = {FriendInteractionListener.class, RetryConfig.class, AsyncConfig.class, FriendAsyncConfig.class})
@Tag("springboot-nodb")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class FriendInteractionListenerRetryTest {

    @Autowired
    private FriendInteractionListener listener;

    @MockitoBean
    private RedisInteractionScoreRepository redisInteractionScoreRepository;

    @MockitoBean
    private FriendEventDlqService friendEventDlqService;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(redisInteractionScoreRepository, friendEventDlqService);
        Mockito.clearInvocations(redisInteractionScoreRepository, friendEventDlqService);
    }

    @Test
    @DisplayName("게시글 좋아요 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handlePostLiked_shouldRetryOnRedisConnectionFailure() {
        // Given
        PostLikedEvent event = PostLikedEvent.of(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong(), anyString());

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisInteractionScoreRepository, times(MAX_ATTEMPTS))
                        .addInteractionScore(eq(3L), eq(2L), eq(event.getIdempotencyKey())));
    }

    @Test
    @DisplayName("게시글 좋아요 - 3회 재시도 실패 후 DLQ 저장")
    void handlePostLiked_shouldSaveToDlqAfterMaxRetries() {
        // Given
        PostLikedEvent event = PostLikedEvent.of(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong(), anyString());

        // When
        listener.handlePostLiked(event);

        // Then: DLQ 저장 호출 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(friendEventDlqService, times(1))
                        .saveScoreUp(eq(event.getIdempotencyKey()), eq(event.getMemberId()), eq(event.getTargetMemberId()), eq(INTERACTION_SCORE_DEFAULT)));
    }

    @Test
    @DisplayName("게시글 좋아요 - 익명 게시글은 재시도 없이 즉시 반환")
    void handlePostLiked_shouldSkipAnonymousPost() {
        // Given - postAuthorId가 null인 익명 게시글
        PostLikedEvent event = PostLikedEvent.of(1L, null, 3L);

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기 - 상호작용 점수 저장 호출 없음
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisInteractionScoreRepository, never()).addInteractionScore(anyLong(), anyLong(), anyString());
                    verify(friendEventDlqService, never()).saveScoreUp(anyString(), anyLong(), anyLong(), anyDouble());
                });
    }

    @Test
    @DisplayName("댓글 작성 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleCommentCreated_shouldRetryOnRedisConnectionFailure() {
        // Given
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong(), anyString());

        // When - 통합된 handlePostLiked 메소드 사용
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisInteractionScoreRepository, times(MAX_ATTEMPTS))
                        .addInteractionScore(eq(event.getMemberId()), eq(event.getTargetMemberId()), eq(event.getIdempotencyKey())));
    }

    @Test
    @DisplayName("댓글 작성 - 3회 재시도 실패 후 DLQ 저장")
    void handleCommentCreated_shouldSaveToDlqAfterMaxRetries() {
        // Given
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong(), anyString());

        // When
        listener.handlePostLiked(event);

        // Then: DLQ 저장 호출 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(friendEventDlqService, times(1))
                        .saveScoreUp(eq(event.getIdempotencyKey()), eq(event.getMemberId()), eq(event.getTargetMemberId()), eq(INTERACTION_SCORE_DEFAULT)));
    }

    @Test
    @DisplayName("댓글 작성 - 익명 댓글은 재시도 없이 즉시 반환")
    void handleCommentCreated_shouldSkipAnonymousComment() {
        // Given - commenterId가 null인 익명 댓글
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", null, 100L);

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기 - 상호작용 점수 저장 호출 없음
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisInteractionScoreRepository, never()).addInteractionScore(anyLong(), anyLong(), anyString());
                    verify(friendEventDlqService, never()).saveScoreUp(anyString(), anyLong(), anyLong(), anyDouble());
                });
    }

    @Test
    @DisplayName("댓글 좋아요 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleCommentLiked_shouldRetryOnRedisConnectionFailure() {
        // Given
        CommentLikeEvent event = CommentLikeEvent.of(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong(), anyString());

        // When - 통합된 handlePostLiked 메소드 사용
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisInteractionScoreRepository, times(MAX_ATTEMPTS))
                        .addInteractionScore(eq(event.getMemberId()), eq(event.getTargetMemberId()), eq(event.getIdempotencyKey())));
    }

    @Test
    @DisplayName("댓글 좋아요 - 3회 재시도 실패 후 DLQ 저장")
    void handleCommentLiked_shouldSaveToDlqAfterMaxRetries() {
        // Given
        CommentLikeEvent event = CommentLikeEvent.of(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong(), anyString());

        // When
        listener.handlePostLiked(event);

        // Then: DLQ 저장 호출 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(friendEventDlqService, times(1))
                        .saveScoreUp(eq(event.getIdempotencyKey()), eq(event.getMemberId()), eq(event.getTargetMemberId()), eq(INTERACTION_SCORE_DEFAULT)));
    }

    @Test
    @DisplayName("댓글 좋아요 - 익명 댓글은 재시도 없이 즉시 반환")
    void handleCommentLiked_shouldSkipAnonymousComment() {
        // Given - commentAuthorId가 null인 익명 댓글
        CommentLikeEvent event = CommentLikeEvent.of(1L, null, 3L);

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기 - 상호작용 점수 저장 호출 없음
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisInteractionScoreRepository, never()).addInteractionScore(anyLong(), anyLong(), anyString());
                    verify(friendEventDlqService, never()).saveScoreUp(anyString(), anyLong(), anyLong(), anyDouble());
                });
    }

    @Test
    @DisplayName("2회 실패 후 3회차에 성공 - DLQ 저장 안함")
    void shouldSucceedAfterTwoFailures_noDlqSave() {
        // Given
        PostLikedEvent event = PostLikedEvent.of(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new RedisConnectionFailureException("실패"))
                .willReturn(true)
                .given(redisInteractionScoreRepository).addInteractionScore(eq(3L), eq(2L), eq(event.getIdempotencyKey()));

        // When
        listener.handlePostLiked(event);

        // Then: 비동기 완료 대기 - DLQ 저장 호출 없음
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(redisInteractionScoreRepository, times(3)).addInteractionScore(eq(3L), eq(2L), eq(event.getIdempotencyKey()));
                    verify(friendEventDlqService, never()).saveScoreUp(anyString(), anyLong(), anyLong(), anyDouble());
                });
    }
}
