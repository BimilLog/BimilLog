package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 조회 이벤트 리스너 테스트</h2>
 * <p>PostViewEventListener의 단위 테스트</p>
 * <p>게시글 조회 이벤트 처리 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 조회 이벤트 리스너 테스트")
class PostViewEventListenerTest {

    @Mock
    private PostInteractionUseCase postInteractionUseCase;

    @InjectMocks
    private PostViewEventListener postViewEventListener;

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 정상적인 조회수 증가")
    void handlePostViewedEvent_ShouldIncrementViewCount() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(this, postId);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 존재하지 않는 게시글")
    void handlePostViewedEvent_WhenPostNotFound_ShouldHandleException() {
        // Given
        Long postId = 999L;
        PostViewedEvent event = new PostViewedEvent(this, postId);
        
        doThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함 (로그만 남기고 예외를 삼킴)
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - null postId (early return)")
    void handlePostViewedEvent_WithNullPostId_ShouldReturnEarly() {
        // Given
        PostViewedEvent event = new PostViewedEvent(this, null);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - null postId인 경우 UseCase 호출하지 않고 early return
        verify(postInteractionUseCase, never()).incrementViewCount(any());
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 다양한 postId 값들")
    void handlePostViewedEvent_WithVariousPostIds() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 999999L;
        Long postId3 = 0L;
        
        PostViewedEvent event1 = new PostViewedEvent(this, postId1);
        PostViewedEvent event2 = new PostViewedEvent(this, postId2);
        PostViewedEvent event3 = new PostViewedEvent(this, postId3);

        // When
        postViewEventListener.handlePostViewedEvent(event1);
        postViewEventListener.handlePostViewedEvent(event2);
        postViewEventListener.handlePostViewedEvent(event3);

        // Then
        verify(postInteractionUseCase).incrementViewCount(eq(postId1));
        verify(postInteractionUseCase).incrementViewCount(eq(postId2));
        verify(postInteractionUseCase).incrementViewCount(eq(postId3));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - 동일 게시글 여러 번 조회")
    void handlePostViewedEvent_SamePostMultipleTimes() {
        // Given
        Long postId = 123L;
        
        PostViewedEvent event1 = new PostViewedEvent(this, postId);
        PostViewedEvent event2 = new PostViewedEvent(this, postId);
        PostViewedEvent event3 = new PostViewedEvent(this, postId);

        // When
        postViewEventListener.handlePostViewedEvent(event1);
        postViewEventListener.handlePostViewedEvent(event2);
        postViewEventListener.handlePostViewedEvent(event3);

        // Then - 매번 호출되어야 함 (중복 검사는 Controller 레이어에서)
        verify(postInteractionUseCase, times(3)).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - UseCase에서 RuntimeException 발생")
    void handlePostViewedEvent_WhenUseCaseThrowsRuntimeException() {
        // Given
        Long postId = 123L;
        PostViewedEvent event = new PostViewedEvent(this, postId);
        
        RuntimeException runtimeException = new RuntimeException("데이터베이스 연결 오류");
        doThrow(runtimeException)
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("게시글 조회 이벤트 처리 - UseCase에서 IllegalArgumentException 발생")
    void handlePostViewedEvent_WhenUseCaseThrowsIllegalArgumentException() {
        // Given
        Long postId = -1L; // 잘못된 postId
        PostViewedEvent event = new PostViewedEvent(this, postId);
        
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("잘못된 게시글 ID");
        doThrow(illegalArgumentException)
                .when(postInteractionUseCase)
                .incrementViewCount(postId);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 예외가 발생해도 메서드는 정상 완료되어야 함
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("대량 이벤트 처리 - 성능 테스트")
    void handlePostViewedEvent_HighVolumeProcessing() {
        // Given
        Long postId = 123L;
        int eventCount = 1000;

        // When - 1000개의 이벤트 처리
        for (int i = 0; i < eventCount; i++) {
            PostViewedEvent event = new PostViewedEvent(this, postId);
            postViewEventListener.handlePostViewedEvent(event);
        }

        // Then
        verify(postInteractionUseCase, times(eventCount)).incrementViewCount(eq(postId));
    }

    @Test
    @DisplayName("UseCase 호출 파라미터 정확성 검증")
    void handlePostViewedEvent_ShouldPassCorrectParameters() {
        // Given
        Long expectedPostId = 789L;
        PostViewedEvent event = new PostViewedEvent(this, expectedPostId);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 정확한 파라미터가 전달되어야 함
        verify(postInteractionUseCase, times(1)).incrementViewCount(eq(expectedPostId));
        
        // 다른 메서드는 호출되지 않아야 함
        verify(postInteractionUseCase, never()).likePost(any(), any());
    }

    @Test
    @DisplayName("이벤트 소스 객체 검증")
    void handlePostViewedEvent_EventSourceValidation() {
        // Given
        Long postId = 123L;
        Object eventSource = new Object();
        PostViewedEvent event = new PostViewedEvent(eventSource, postId);
        
        // 이벤트 소스 검증
        assert event.getSource().equals(eventSource);

        // When
        postViewEventListener.handlePostViewedEvent(event);

        // Then - 이벤트 소스와 관계없이 정상 처리되어야 함
        verify(postInteractionUseCase).incrementViewCount(eq(postId));
    }
}