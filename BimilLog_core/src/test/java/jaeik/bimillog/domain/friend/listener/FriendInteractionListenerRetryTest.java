package jaeik.bimillog.domain.friend.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentLikeEvent;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
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
 * <h2>FriendInteractionListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("FriendInteractionListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {FriendInteractionListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
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

    private static final int MAX_ATTEMPTS = 3;

    @Test
    @DisplayName("게시글 좋아요 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handlePostLiked_shouldRetryOnRedisConnectionFailure() {
        // Given
        PostLikeEvent event = new PostLikeEvent(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong());

        // When
        listener.handlePostLiked(event);

        // Then
        verify(redisInteractionScoreRepository, times(MAX_ATTEMPTS))
                .addInteractionScore(2L, 3L);
    }

    @Test
    @DisplayName("게시글 좋아요 - 익명 게시글은 재시도 없이 즉시 반환")
    void handlePostLiked_shouldSkipAnonymousPost() {
        // Given - postAuthorId가 null인 익명 게시글
        PostLikeEvent event = new PostLikeEvent(1L, null, 3L);

        // When
        listener.handlePostLiked(event);

        // Then - 상호작용 점수 저장 호출 없음
        verify(redisInteractionScoreRepository, never())
                .addInteractionScore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("댓글 작성 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleCommentCreated_shouldRetryOnRedisConnectionFailure() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong());

        // When
        listener.handleCommentCreated(event);

        // Then
        verify(redisInteractionScoreRepository, times(MAX_ATTEMPTS))
                .addInteractionScore(1L, 2L);
    }

    @Test
    @DisplayName("댓글 작성 - 익명 댓글은 재시도 없이 즉시 반환")
    void handleCommentCreated_shouldSkipAnonymousComment() {
        // Given - commenterId가 null인 익명 댓글
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", null, 100L);

        // When
        listener.handleCommentCreated(event);

        // Then - 상호작용 점수 저장 호출 없음
        verify(redisInteractionScoreRepository, never())
                .addInteractionScore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("댓글 좋아요 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleCommentLiked_shouldRetryOnRedisConnectionFailure() {
        // Given
        CommentLikeEvent event = new CommentLikeEvent(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisInteractionScoreRepository).addInteractionScore(anyLong(), anyLong());

        // When
        listener.handleCommentLiked(event);

        // Then
        verify(redisInteractionScoreRepository, times(MAX_ATTEMPTS))
                .addInteractionScore(2L, 3L);
    }

    @Test
    @DisplayName("댓글 좋아요 - 익명 댓글은 재시도 없이 즉시 반환")
    void handleCommentLiked_shouldSkipAnonymousComment() {
        // Given - commentAuthorId가 null인 익명 댓글
        CommentLikeEvent event = new CommentLikeEvent(1L, null, 3L);

        // When
        listener.handleCommentLiked(event);

        // Then - 상호작용 점수 저장 호출 없음
        verify(redisInteractionScoreRepository, never())
                .addInteractionScore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("2회 실패 후 3회차에 성공")
    void shouldSucceedAfterTwoFailures() {
        // Given
        PostLikeEvent event = new PostLikeEvent(1L, 2L, 3L);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new RedisConnectionFailureException("실패"))
                .willDoNothing()
                .given(redisInteractionScoreRepository).addInteractionScore(2L, 3L);

        // When
        listener.handlePostLiked(event);

        // Then
        verify(redisInteractionScoreRepository, times(3))
                .addInteractionScore(2L, 3L);
    }
}
