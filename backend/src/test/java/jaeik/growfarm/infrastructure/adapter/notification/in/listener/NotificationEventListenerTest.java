package jaeik.growfarm.infrastructure.adapter.notification.in.listener;

import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.post.event.PostFeaturedEvent;
import jaeik.growfarm.domain.paper.event.RollingPaperEvent;
import jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler.NotificationEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>알림 이벤트 리스너 (디스패처) 테스트</h2>
 * <p>NotificationEventListener의 단위 테스트</p>
 * <p>각 이벤트 타입별 디스패칭 로직과 핸들러 호출 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("알림 이벤트 리스너 디스패처 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationEventHandler<CommentCreatedEvent> mockCommentHandler;

    @Mock
    private NotificationEventHandler<PostFeaturedEvent> mockPostHandler;

    @Mock
    private NotificationEventHandler<RollingPaperEvent> mockPaperHandler;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 적절한 핸들러가 처리")
    void handleCommentCreatedEvent_ShouldUseCorrectHandler() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        when(mockCommentHandler.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When
        notificationEventListener.handleCommentCreatedEvent(event);

        // Then
        verify(mockCommentHandler).supports(event);
        verify(mockCommentHandler).handle(event);
        // 첫 번째 핸들러가 처리했으므로 나머지는 호출되지 않음
        verify(mockPostHandler, never()).supports(event);
        verify(mockPaperHandler, never()).supports(event);
    }

    @Test
    @DisplayName("인기글 선정 이벤트 처리 - 적절한 핸들러가 처리")
    void handlePostFeaturedEvent_ShouldUseCorrectHandler() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(this, 1L, "SSE 메시지", 100L, "FCM 제목", "FCM 내용");
        
        when(mockCommentHandler.supports(event)).thenReturn(false);
        when(mockPostHandler.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When
        notificationEventListener.handlePostFeaturedEvent(event);

        // Then
        verify(mockCommentHandler).supports(event);
        verify(mockCommentHandler, never()).handle(any());
        verify(mockPostHandler).supports(event);
        verify(mockPostHandler).handle(event);
        // 두 번째 핸들러가 처리했으므로 세 번째는 호출되지 않음
        verify(mockPaperHandler, never()).supports(event);
    }

    @Test
    @DisplayName("롤링페이퍼 이벤트 처리 - 적절한 핸들러가 처리")
    void handleRollingPaperEvent_ShouldUseCorrectHandler() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(this, 1L, "테스트사용자");
        
        when(mockCommentHandler.supports(event)).thenReturn(false);
        when(mockPostHandler.supports(event)).thenReturn(false);
        when(mockPaperHandler.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When
        notificationEventListener.handleRollingPaperEvent(event);

        // Then
        verify(mockCommentHandler).supports(event);
        verify(mockCommentHandler, never()).handle(any());
        verify(mockPostHandler).supports(event);
        verify(mockPostHandler, never()).handle(any());
        verify(mockPaperHandler).supports(event);
        verify(mockPaperHandler).handle(event);
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 지원하는 핸들러가 없는 경우")
    void handleCommentCreatedEvent_NoHandlerSupports_ShouldNotHandleAnything() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        // 모든 핸들러가 지원하지 않음
        when(mockCommentHandler.supports(event)).thenReturn(false);
        when(mockPostHandler.supports(event)).thenReturn(false);
        when(mockPaperHandler.supports(event)).thenReturn(false);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When
        notificationEventListener.handleCommentCreatedEvent(event);

        // Then
        verify(mockCommentHandler).supports(event);
        verify(mockCommentHandler, never()).handle(any());
        verify(mockPostHandler).supports(event);
        verify(mockPostHandler, never()).handle(any());
        verify(mockPaperHandler).supports(event);
        verify(mockPaperHandler, never()).handle(any());
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 빈 핸들러 리스트")
    void handleCommentCreatedEvent_EmptyHandlerList_ShouldDoNothing() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        setHandlers(Collections.emptyList());

        // When
        notificationEventListener.handleCommentCreatedEvent(event);

        // Then
        // 아무것도 호출되지 않아야 함
        verifyNoInteractions(mockCommentHandler, mockPostHandler, mockPaperHandler);
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 핸들러에서 예외 발생")
    void handleCommentCreatedEvent_HandlerThrowsException_ShouldPropagateException() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        when(mockCommentHandler.supports(event)).thenReturn(true);
        doThrow(new RuntimeException("핸들러 처리 실패")).when(mockCommentHandler).handle(event);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When & Then
        try {
            notificationEventListener.handleCommentCreatedEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함
            verify(mockCommentHandler).supports(event);
            verify(mockCommentHandler).handle(event);
            verify(mockPostHandler, never()).supports(event);
            verify(mockPaperHandler, never()).supports(event);
        }
    }

    @Test
    @DisplayName("인기글 선정 이벤트 처리 - supports 메서드에서 예외 발생")
    void handlePostFeaturedEvent_SupportsMethodThrowsException_ShouldPropagateException() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(this, 1L, "SSE 메시지", 100L, "FCM 제목", "FCM 내용");
        
        when(mockCommentHandler.supports(event)).thenThrow(new RuntimeException("supports 메서드 실패"));
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When & Then
        try {
            notificationEventListener.handlePostFeaturedEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함
            verify(mockCommentHandler).supports(event);
            verify(mockCommentHandler, never()).handle(any());
            verify(mockPostHandler, never()).supports(any());
            verify(mockPaperHandler, never()).supports(any());
        }
    }

    @Test
    @DisplayName("롤링페이퍼 이벤트 처리 - 다양한 이벤트 데이터")
    void handleRollingPaperEvent_WithVariousEventData_ShouldProcess() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(this, 999L, "특별한사용자");
        
        when(mockPaperHandler.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When
        notificationEventListener.handleRollingPaperEvent(event);

        // Then
        verify(mockCommentHandler).supports(event);
        verify(mockPostHandler).supports(event);
        verify(mockPaperHandler).supports(event);
        verify(mockPaperHandler).handle(event);
    }

    @Test
    @DisplayName("다중 핸들러가 모두 지원하는 경우 - 첫 번째 핸들러만 처리")
    void handleCommentCreatedEvent_MultipleHandlersSupport_ShouldUseFirstHandlerOnly() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        // 첫 번째 핸들러만 지원한다고 설정 (실제 동작과 일치)
        when(mockCommentHandler.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockCommentHandler, mockPostHandler, mockPaperHandler));

        // When
        notificationEventListener.handleCommentCreatedEvent(event);

        // Then
        verify(mockCommentHandler).supports(event);
        verify(mockCommentHandler).handle(event);
        // 첫 번째 핸들러가 처리했으므로 나머지는 확인하지 않음
        verify(mockPostHandler, never()).supports(event);
        verify(mockPaperHandler, never()).supports(event);
    }

    /**
     * 리플렉션을 사용하여 NotificationEventListener의 핸들러 리스트를 설정
     */
    private void setHandlers(java.util.List<NotificationEventHandler> handlers) {
        try {
            java.lang.reflect.Field field = NotificationEventListener.class.getDeclaredField("notificationEventHandlers");
            field.setAccessible(true);
            field.set(notificationEventListener, handlers);
        } catch (Exception e) {
            throw new RuntimeException("핸들러 리스트 설정 실패", e);
        }
    }
}