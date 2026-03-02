package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.event.PostEvent.PostDetailViewedEvent;
import jaeik.bimillog.domain.post.event.PostLikedEvent;
import jaeik.bimillog.domain.post.event.PostEvent.PostUnlikedEvent;
import jaeik.bimillog.domain.post.event.PostEvent.RealtimeCacheRebuildEvent;
import jaeik.bimillog.domain.post.listener.RealtimeUpdateListener;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>RealtimeUpdateListener 단위 테스트</h2>
 * <p>실시간 인기글 점수 리스너의 이벤트 처리 로직을 검증합니다.</p>
 * <p>서킷브레이커 동작은 RedisRealTimePostAdapter에서 테스트합니다.</p>
 *
 * @author Jaeik
 */
@Tag("unit")
@DisplayName("RealtimeUpdateListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RealtimeUpdateListenerTest {

    @Mock
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Mock
    private RedisPostListUpdateAdapter redisPostListUpdateAdapter;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private RealtimeUpdateListener listener;

    @Test
    @DisplayName("댓글 작성 이벤트 - 점수 3점 증가")
    void handleCommentCreated_shouldIncrementScoreByThree() {
        // Given
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(100L, 3.0);
    }

    @Test
    @DisplayName("게시글 추천 이벤트 - 점수 4점 증가")
    void handlePostLiked_shouldIncrementScoreByFour() {
        // Given
        PostLikedEvent event = PostLikedEvent.of(1L, null, null);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, 4.0);
    }

    @Test
    @DisplayName("게시글 추천취소 이벤트 - 점수 4점 감소")
    void handlePostUnliked_shouldDecrementScoreByFour() {
        // Given
        PostUnlikedEvent event = new PostUnlikedEvent(1L);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, -4.0);
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 - 점수 3점 감소")
    void handleCommentDeleted_shouldDecrementScoreByThree() {
        // Given
        CommentDeletedEvent event = new CommentDeletedEvent(100L);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(100L, -3.0);
    }

    @Test
    @DisplayName("게시글 상세 조회 이벤트 - 점수 2점 증가")
    void handlePostDetailViewed_shouldIncrementScore() {
        // Given
        PostDetailViewedEvent event = new PostDetailViewedEvent(1L, "test-viewer");

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, 2.0);
    }

    @Test
    @DisplayName("실시간 캐시 리빌드 이벤트 - DB 조회 후 LIST 교체")
    void handleRealtimeCacheRebuild_shouldReplaceList() {
        // Given
        List<Long> postIds = List.of(3L, 1L, 2L);
        RealtimeCacheRebuildEvent event = new RealtimeCacheRebuildEvent(postIds);

        Post post1 = PostTestDataBuilder.withId(1L, PostTestDataBuilder.createPost(null, "글1", "내용1"));
        Post post2 = PostTestDataBuilder.withId(2L, PostTestDataBuilder.createPost(null, "글2", "내용2"));
        Post post3 = PostTestDataBuilder.withId(3L, PostTestDataBuilder.createPost(null, "글3", "내용3"));

        given(postRepository.findAllByIds(postIds)).willReturn(List.of(post3, post1, post2));

        // When
        listener.handleRealtimeCacheRebuild(event);

        // Then
        verify(postRepository).findAllByIds(postIds);
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.POST_REALTIME_JSON_KEY), any(), eq(RedisKey.DEFAULT_CACHE_TTL));
    }

    @Test
    @DisplayName("실시간 캐시 리빌드 이벤트 - DB 결과 비어있으면 LIST 교체하지 않음")
    void handleRealtimeCacheRebuild_shouldSkip_WhenDbResultEmpty() {
        // Given
        List<Long> postIds = List.of(999L);
        RealtimeCacheRebuildEvent event = new RealtimeCacheRebuildEvent(postIds);

        given(postRepository.findAllByIds(postIds)).willReturn(List.of());

        // When
        listener.handleRealtimeCacheRebuild(event);

        // Then
        verify(postRepository).findAllByIds(postIds);
        verify(redisPostListUpdateAdapter, never()).replaceList(any(), any(), any());
    }
}
