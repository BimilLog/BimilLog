package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.out.NotificationUrlPort;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationSseService 테스트</h2>
 * <p>SSE 알림 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>EventDTO 제거 후 NotificationEvent 도메인 엔티티를 사용하도록 리팩토링됨</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSseService 테스트")
class NotificationSseServiceTest {

    @Mock
    private SsePort ssePort;

    @Mock
    private NotificationUrlPort notificationUrlPort;

    @InjectMocks
    private NotificationSseService notificationSseService;

    @Test
    @DisplayName("알림 구독 - 성공")
    void shouldSubscribe_WhenValidInput() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        SseEmitter expectedEmitter = new SseEmitter();
        
        given(ssePort.subscribe(userId, tokenId)).willReturn(expectedEmitter);

        // When
        SseEmitter actualEmitter = notificationSseService.subscribe(userId, tokenId);

        // Then
        assertThat(actualEmitter).isEqualTo(expectedEmitter);
        verify(ssePort).subscribe(userId, tokenId);
    }

    @Test
    @DisplayName("사용자 SSE 연결 정리 - 성공")
    void shouldDeleteAllEmitterByUserId_WhenValidUserId() {
        // Given
        Long userId = 1L;

        // When
        notificationSseService.deleteAllEmitterByUserId(userId);

        // Then
        verify(ssePort).deleteAllEmitterByUserId(userId);
    }

    @Test
    @DisplayName("댓글 알림 SSE 전송 - 성공")
    void shouldSendCommentNotification_WhenValidInput() {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스터";
        Long postId = 100L;
        String expectedMessage = commenterName + "님이 댓글을 남겼습니다!";
        String expectedUrl = "/board/post/100";
        
        given(notificationUrlPort.generatePostUrl(postId)).willReturn(expectedUrl);

        // When
        notificationSseService.sendCommentNotification(postUserId, commenterName, postId);

        // Then
        verify(notificationUrlPort).generatePostUrl(postId);
        verify(ssePort).send(eq(postUserId), argThat(event ->
                event.getType() == NotificationType.COMMENT &&
                event.getMessage().equals(expectedMessage) &&
                event.getUrl().equals(expectedUrl)
        ));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 SSE 전송 - 성공")
    void shouldSendPaperPlantNotification_WhenValidInput() {
        // Given
        Long farmOwnerId = 1L;
        String userName = "testuser";
        String expectedMessage = "롤링페이퍼에 메시지가 작성되었어요!";
        String expectedUrl = "/rolling-paper/testuser";
        
        given(notificationUrlPort.generateRollingPaperUrl(userName)).willReturn(expectedUrl);

        // When
        notificationSseService.sendPaperPlantNotification(farmOwnerId, userName);

        // Then
        verify(notificationUrlPort).generateRollingPaperUrl(userName);
        verify(ssePort).send(eq(farmOwnerId), argThat(event ->
                event.getType() == NotificationType.PAPER &&
                event.getMessage().equals(expectedMessage) &&
                event.getUrl().equals(expectedUrl)
        ));
    }

    @Test
    @DisplayName("인기글 등극 알림 SSE 전송 - 성공")
    void shouldSendPostFeaturedNotification_WhenValidInput() {
        // Given
        Long userId = 1L;
        String message = "축하합니다! 회원님의 글이 인기글에 등극했습니다!";
        Long postId = 100L;
        String expectedUrl = "/board/post/100";
        
        given(notificationUrlPort.generatePostUrl(postId)).willReturn(expectedUrl);

        // When
        notificationSseService.sendPostFeaturedNotification(userId, message, postId);

        // Then
        verify(notificationUrlPort).generatePostUrl(postId);
        verify(ssePort).send(eq(userId), argThat(event ->
                event.getType() == NotificationType.POST_FEATURED &&
                event.getMessage().equals(message) &&
                event.getUrl().equals(expectedUrl)
        ));
    }
}