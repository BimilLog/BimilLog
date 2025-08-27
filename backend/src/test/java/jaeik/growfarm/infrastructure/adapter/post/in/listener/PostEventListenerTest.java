package jaeik.growfarm.infrastructure.adapter.post.in.listener;

import jaeik.growfarm.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.growfarm.domain.post.event.PostDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 이벤트 리스너 테스트</h2>
 * <p>PostEventListener의 단위 테스트</p>
 * <p>게시글 삭제 이벤트 처리 시 추천 삭제 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 이벤트 리스너 테스트")
class PostEventListenerTest {

    @Mock
    private PostLikeCommandPort postLikeCommandPort;

    @InjectMocks
    private PostEventListener postEventListener;

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 추천 삭제 성공")
    void handlePostDeletedEvent_ShouldDeleteAllPostLikes() {
        // Given
        Long postId = 100L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "테스트 게시글");

        // When
        postEventListener.handlePostDeletedEvent(event);

        // Then
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 추천 삭제 실패 시 예외 로깅")
    void handlePostDeletedEvent_WhenDeleteFails_ShouldLogError() {
        // Given
        Long postId = 200L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "실패 테스트 게시글");
        
        RuntimeException deleteException = new RuntimeException("데이터베이스 연결 실패");
        doThrow(deleteException).when(postLikeCommandPort).deleteAllByPostId(postId);

        // When - 예외가 발생해도 리스너는 정상적으로 완료되어야 함 (로그 기록 후)
        postEventListener.handlePostDeletedEvent(event);

        // Then
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 다양한 게시글 ID")
    void handlePostDeletedEvent_WithDifferentPostIds() {
        // Given
        Long postId1 = 500L;
        Long postId2 = 777L;
        Long postId3 = 999L;
        PostDeletedEvent event1 = new PostDeletedEvent(postId1, "게시글1");
        PostDeletedEvent event2 = new PostDeletedEvent(postId2, "게시글2");
        PostDeletedEvent event3 = new PostDeletedEvent(postId3, "게시글3");
        
        // When
        postEventListener.handlePostDeletedEvent(event1);
        postEventListener.handlePostDeletedEvent(event2);
        postEventListener.handlePostDeletedEvent(event3);
        
        // Then
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId1));
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId2));
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId3));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - null postId 처리")
    void handlePostDeletedEvent_WithNullPostId() {
        // Given
        PostDeletedEvent event = new PostDeletedEvent(null, "null ID 게시글");

        // When
        postEventListener.handlePostDeletedEvent(event);

        // Then - null postId도 포트로 전달되어야 함
        verify(postLikeCommandPort).deleteAllByPostId(eq(null));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 대량 추천이 있는 게시글")
    void handlePostDeletedEvent_WithManyLikes() {
        // Given
        Long popularPostId = 12345L;
        PostDeletedEvent event = new PostDeletedEvent(popularPostId, "인기 게시글");

        // When
        postEventListener.handlePostDeletedEvent(event);

        // Then
        verify(postLikeCommandPort).deleteAllByPostId(eq(popularPostId));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 이벤트 데이터 검증")
    void handlePostDeletedEvent_EventDataValidation() {
        // Given
        Long postId = 888L;
        String postTitle = "데이터 검증용 게시글";
        PostDeletedEvent event = new PostDeletedEvent(postId, postTitle);
        
        // Event 데이터 검증
        assert event.postId().equals(postId);
        assert event.postTitle().equals(postTitle);

        // When
        postEventListener.handlePostDeletedEvent(event);

        // Then
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 멀티스레드 환경 시뮬레이션")
    void handlePostDeletedEvent_ConcurrentEvents() throws InterruptedException {
        // Given
        Long postId1 = 1001L;
        Long postId2 = 1002L;
        PostDeletedEvent event1 = new PostDeletedEvent(postId1, "동시성 테스트1");
        PostDeletedEvent event2 = new PostDeletedEvent(postId2, "동시성 테스트2");

        // When - 동시 처리 시뮬레이션
        Thread thread1 = new Thread(() -> postEventListener.handlePostDeletedEvent(event1));
        Thread thread2 = new Thread(() -> postEventListener.handlePostDeletedEvent(event2));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId1));
        verify(postLikeCommandPort).deleteAllByPostId(eq(postId2));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 연속 호출")
    void handlePostDeletedEvent_ConsecutiveCalls() {
        // Given
        Long postId = 2000L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "연속 호출 테스트");

        // When - 같은 이벤트를 여러 번 처리
        postEventListener.handlePostDeletedEvent(event);
        postEventListener.handlePostDeletedEvent(event);

        // Then - 각 호출마다 포트가 호출되어야 함
        verify(postLikeCommandPort, times(2)).deleteAllByPostId(eq(postId));
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 처리 - 빈 제목 처리")
    void handlePostDeletedEvent_WithEmptyTitle() {
        // Given
        Long postId = 3000L;
        PostDeletedEvent eventWithEmptyTitle = new PostDeletedEvent(postId, "");
        PostDeletedEvent eventWithNullTitle = new PostDeletedEvent(postId, null);

        // When
        postEventListener.handlePostDeletedEvent(eventWithEmptyTitle);
        postEventListener.handlePostDeletedEvent(eventWithNullTitle);

        // Then
        verify(postLikeCommandPort, times(2)).deleteAllByPostId(eq(postId));
    }
}