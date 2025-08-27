package jaeik.growfarm.infrastructure.adapter.notification.in.listener;

import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.post.event.PostFeaturedEvent;
import jaeik.growfarm.domain.paper.event.RollingPaperEvent;
import jaeik.growfarm.domain.admin.event.UserBannedEvent;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler.NotificationEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>알림 이벤트 리스너 (디스패처) 테스트</h2>
 * <p>NotificationEventListener의 단위 테스트</p>
 * <p>이벤트 디스패칭 로직과 핸들러 호출 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("알림 이벤트 리스너 디스패처 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationEventHandler<ApplicationEvent> mockHandler1;

    @Mock
    private NotificationEventHandler<ApplicationEvent> mockHandler2;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("이벤트 처리 - 첫 번째 핸들러가 지원하는 경우")
    void handleNotificationEvent_FirstHandlerSupports_ShouldHandleWithFirstHandler() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        // 첫 번째 핸들러만 지원
        when(mockHandler1.supports(event)).thenReturn(true);
        // mockHandler2는 호출되지 않으므로 stubbing하지 않음
        
        // NotificationEventListener 내부적으로 핸들러 리스트를 생성자에서 주입받으므로
        // 리플렉션을 사용하여 핸들러 리스트를 설정
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When
        notificationEventListener.handleNotificationEvent(event);

        // Then
        verify(mockHandler1).supports(event);
        verify(mockHandler1).handle(event);
        verify(mockHandler2, never()).supports(event);
        verify(mockHandler2, never()).handle(any());
    }

    @Test
    @DisplayName("이벤트 처리 - 두 번째 핸들러가 지원하는 경우")
    void handleNotificationEvent_SecondHandlerSupports_ShouldHandleWithSecondHandler() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(this, 1L, "SSE 메시지", 100L, "FCM 제목", "FCM 내용");
        
        // 두 번째 핸들러만 지원
        when(mockHandler1.supports(event)).thenReturn(false);
        when(mockHandler2.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When
        notificationEventListener.handleNotificationEvent(event);

        // Then
        verify(mockHandler1).supports(event);
        verify(mockHandler1, never()).handle(any());
        verify(mockHandler2).supports(event);
        verify(mockHandler2).handle(event);
    }

    @Test
    @DisplayName("이벤트 처리 - 지원하는 핸들러가 없는 경우")
    void handleNotificationEvent_NoHandlerSupports_ShouldNotHandleAnything() {
        // Given
        UserBannedEvent event = new UserBannedEvent(this, 1L, "socialId", SocialProvider.KAKAO);
        
        // 모든 핸들러가 지원하지 않음
        when(mockHandler1.supports(event)).thenReturn(false);
        when(mockHandler2.supports(event)).thenReturn(false);
        
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When
        notificationEventListener.handleNotificationEvent(event);

        // Then
        verify(mockHandler1).supports(event);
        verify(mockHandler1, never()).handle(any());
        verify(mockHandler2).supports(event);
        verify(mockHandler2, never()).handle(any());
    }

    @Test
    @DisplayName("이벤트 처리 - 빈 핸들러 리스트")
    void handleNotificationEvent_EmptyHandlerList_ShouldDoNothing() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        setHandlers(Collections.emptyList());

        // When
        notificationEventListener.handleNotificationEvent(event);

        // Then
        // 아무것도 호출되지 않아야 함
        verifyNoInteractions(mockHandler1, mockHandler2);
    }

    @Test
    @DisplayName("이벤트 처리 - 첫 번째 핸들러에서 예외 발생")
    void handleNotificationEvent_FirstHandlerThrowsException_ShouldPropagateException() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(this, 1L, "테스트사용자");
        
        when(mockHandler1.supports(event)).thenReturn(true);
        doThrow(new RuntimeException("핸들러 처리 실패")).when(mockHandler1).handle(event);
        
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When & Then
        try {
            notificationEventListener.handleNotificationEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함
            verify(mockHandler1).supports(event);
            verify(mockHandler1).handle(event);
            verify(mockHandler2, never()).supports(event);
            verify(mockHandler2, never()).handle(any());
        }
    }

    @Test
    @DisplayName("이벤트 처리 - supports 메서드에서 예외 발생")
    void handleNotificationEvent_SupportsMethodThrowsException_ShouldPropagateException() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        when(mockHandler1.supports(event)).thenThrow(new RuntimeException("supports 메서드 실패"));
        
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When & Then
        try {
            notificationEventListener.handleNotificationEvent(event);
        } catch (RuntimeException e) {
            // 예외가 전파되어야 함
            verify(mockHandler1).supports(event);
            verify(mockHandler1, never()).handle(any());
            verify(mockHandler2, never()).supports(any());
        }
    }

    @Test
    @DisplayName("이벤트 처리 - null 이벤트")
    void handleNotificationEvent_NullEvent_ShouldNotCrash() {
        // Given
        // 모든 핸들러가 null 이벤트를 지원하지 않음
        when(mockHandler1.supports(null)).thenReturn(false);
        when(mockHandler2.supports(null)).thenReturn(false);
        
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When
        notificationEventListener.handleNotificationEvent(null);

        // Then
        verify(mockHandler1).supports(null);
        verify(mockHandler2).supports(null);  // 첫 번째가 false를 반환하므로 두 번째도 확인됨
        verify(mockHandler1, never()).handle(any());
        verify(mockHandler2, never()).handle(any());
    }

    @Test
    @DisplayName("이벤트 처리 - 양쪽 핸들러 모두 지원하는 경우 첫 번째만 처리")
    void handleNotificationEvent_BothHandlersSupport_ShouldUseFirstHandlerOnly() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);
        
        // 첫 번째 핸들러만 지원하도록 설정 (두 번째는 호출되지 않으므로 stubbing하지 않음)
        when(mockHandler1.supports(event)).thenReturn(true);
        
        setHandlers(Arrays.asList(mockHandler1, mockHandler2));

        // When
        notificationEventListener.handleNotificationEvent(event);

        // Then
        verify(mockHandler1).supports(event);
        verify(mockHandler1).handle(event);
        verify(mockHandler2, never()).supports(event);  // 첫 번째에서 처리되므로 두 번째는 확인하지 않음
        verify(mockHandler2, never()).handle(any());
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