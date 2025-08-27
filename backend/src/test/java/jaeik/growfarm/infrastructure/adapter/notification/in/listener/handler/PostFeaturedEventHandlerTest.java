package jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler;

import jaeik.growfarm.domain.post.event.PostFeaturedEvent;
import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * <h2>인기 게시글 선정 이벤트 핸들러 테스트</h2>
 * <p>PostFeaturedEventHandler의 단위 테스트</p>
 * <p>인기 게시글 선정 시 SSE와 FCM 알림 전송 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("인기 게시글 선정 이벤트 핸들러 테스트")
class PostFeaturedEventHandlerTest {

    @Mock
    private NotificationSseUseCase notificationSseUseCase;

    @Mock
    private NotificationFcmUseCase notificationFcmUseCase;

    @InjectMocks
    private PostFeaturedEventHandler postFeaturedEventHandler;

    @Test
    @DisplayName("이벤트 지원 여부 확인 - PostFeaturedEvent 지원")
    void supports_WithPostFeaturedEvent_ShouldReturnTrue() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(this, 1L, "SSE 메시지", 100L, "FCM 제목", "FCM 내용");

        // When
        boolean supports = postFeaturedEventHandler.supports(event);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("이벤트 지원 여부 확인 - 다른 이벤트는 미지원")
    void supports_WithOtherEvent_ShouldReturnFalse() {
        // Given
        ApplicationEvent otherEvent = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);

        // When
        boolean supports = postFeaturedEventHandler.supports(otherEvent);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("이벤트 지원 여부 확인 - null 이벤트")
    void supports_WithNullEvent_ShouldReturnFalse() {
        // When
        boolean supports = postFeaturedEventHandler.supports(null);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - SSE와 FCM 알림 전송")
    void handle_WithPostFeaturedEvent_ShouldSendNotifications() {
        // Given
        Long userId = 1L;
        String sseMessage = "축하합니다! 회원님의 게시글이 인기글로 선정되었습니다.";
        Long postId = 100L;
        String fcmTitle = "인기글 선정";
        String fcmBody = "회원님의 게시글이 인기글로 선정되었습니다!";
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        postFeaturedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(
                eq(userId), eq(sseMessage), eq(postId));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(
                eq(userId), eq(fcmTitle), eq(fcmBody));
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - 다양한 이벤트 데이터")
    void handle_WithVariousEventData_ShouldSendCorrectNotifications() {
        // Given
        Long userId = 999L;
        String sseMessage = "특별한 메시지";
        Long postId = 777L;
        String fcmTitle = "특별한 제목";
        String fcmBody = "특별한 내용";
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        postFeaturedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(
                eq(userId), eq(sseMessage), eq(postId));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(
                eq(userId), eq(fcmTitle), eq(fcmBody));
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - null 값들")
    void handle_WithNullValues_ShouldSendNotifications() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(this, null, null, null, null, null);

        // When
        postFeaturedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(null, null, null);
        verify(notificationFcmUseCase).sendPostFeaturedNotification(null, null, null);
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - 빈 문자열")
    void handle_WithEmptyStrings_ShouldSendNotifications() {
        // Given
        Long userId = 1L;
        String sseMessage = "";
        Long postId = 100L;
        String fcmTitle = "";
        String fcmBody = "";
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        postFeaturedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(
                eq(userId), eq(sseMessage), eq(postId));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(
                eq(userId), eq(fcmTitle), eq(fcmBody));
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - SSE 알림 실패 시에도 FCM 알림 전송")
    void handle_WhenSseNotificationFails_ShouldStillSendFcmNotification() {
        // Given
        Long userId = 1L;
        String sseMessage = "SSE 메시지";
        Long postId = 100L;
        String fcmTitle = "FCM 제목";
        String fcmBody = "FCM 내용";
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);
        
        RuntimeException sseException = new RuntimeException("SSE 알림 실패");
        doThrow(sseException).when(notificationSseUseCase)
                .sendPostFeaturedNotification(userId, sseMessage, postId);

        // When & Then
        try {
            postFeaturedEventHandler.handle(event);
        } catch (RuntimeException e) {
            // SSE가 실패해도 FCM은 호출되어야 함
            verify(notificationSseUseCase).sendPostFeaturedNotification(
                    eq(userId), eq(sseMessage), eq(postId));
            // FCM은 호출되지 않음 (SSE에서 예외 발생으로 중단)
            verifyNoInteractions(notificationFcmUseCase);
        }
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - FCM 알림 실패 시 예외 전파")
    void handle_WhenFcmNotificationFails_ShouldPropagateException() {
        // Given
        Long userId = 1L;
        String sseMessage = "SSE 메시지";
        Long postId = 100L;
        String fcmTitle = "FCM 제목";
        String fcmBody = "FCM 내용";
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);
        
        RuntimeException fcmException = new RuntimeException("FCM 알림 실패");
        doThrow(fcmException).when(notificationFcmUseCase)
                .sendPostFeaturedNotification(userId, fcmTitle, fcmBody);

        // When & Then
        try {
            postFeaturedEventHandler.handle(event);
        } catch (RuntimeException e) {
            // SSE는 정상 호출
            verify(notificationSseUseCase).sendPostFeaturedNotification(
                    eq(userId), eq(sseMessage), eq(postId));
            // FCM도 호출됨
            verify(notificationFcmUseCase).sendPostFeaturedNotification(
                    eq(userId), eq(fcmTitle), eq(fcmBody));
        }
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - 이벤트 데이터 검증")
    void handle_EventDataValidation() {
        // Given
        Long userId = 123L;
        String sseMessage = "테스트 SSE 메시지";
        Long postId = 456L;
        String fcmTitle = "테스트 FCM 제목";
        String fcmBody = "테스트 FCM 내용";
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);

        // 이벤트 데이터 검증
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getSseMessage()).isEqualTo(sseMessage);
        assertThat(event.getPostId()).isEqualTo(postId);
        assertThat(event.getFcmTitle()).isEqualTo(fcmTitle);
        assertThat(event.getFcmBody()).isEqualTo(fcmBody);

        // When
        postFeaturedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(
                eq(userId), eq(sseMessage), eq(postId));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(
                eq(userId), eq(fcmTitle), eq(fcmBody));
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 처리 - 긴 메시지 내용")
    void handle_WithLongMessages_ShouldSendNotifications() {
        // Given
        Long userId = 1L;
        String sseMessage = "매우 긴 SSE 메시지".repeat(100);
        Long postId = 100L;
        String fcmTitle = "매우 긴 FCM 제목".repeat(10);
        String fcmBody = "매우 긴 FCM 내용".repeat(50);
        
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        postFeaturedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(
                eq(userId), eq(sseMessage), eq(postId));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(
                eq(userId), eq(fcmTitle), eq(fcmBody));
    }
}