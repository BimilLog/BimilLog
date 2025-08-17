package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.out.NotificationSender;
import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.domain.notification.application.port.out.SsePort;
import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.EventDTO;
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
 *
 * @author Jaeik
 * @version 2.0.0
 * 
 *
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSseService 테스트")
class NotificationSseServiceTest {

    @Mock
    private SsePort ssePort;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationUrlGenerator urlGenerator;

    @Mock
    private NotificationUtilPort notificationUtilPort;

    @InjectMocks
    private NotificationSseService notificationSseService;

    @Test
    @DisplayName("알림 구독 - 성공")
    void shouldSubscribe_WhenValidInput() {
        // Given
        Long userId = 1L;
        Long tokenId = 123L;
        SseEmitter expectedEmitter = new SseEmitter();
        
        given(ssePort.subscribe(userId, tokenId)).willReturn(expectedEmitter);

        // When
        SseEmitter result = notificationSseService.subscribe(userId, tokenId);

        // Then
        assertThat(result).isEqualTo(expectedEmitter);
        verify(ssePort, times(1)).subscribe(userId, tokenId);
        verifyNoMoreInteractions(ssePort);
    }

    @Test
    @DisplayName("사용자 SSE 연결 정리 - 성공")
    void shouldDeleteAllEmitterByUserId_WhenValidUserId() {
        // Given
        Long userId = 1L;

        // When
        notificationSseService.deleteAllEmitterByUserId(userId);

        // Then
        verify(ssePort, times(1)).deleteAllEmitterByUserId(userId);
        verifyNoMoreInteractions(ssePort);
    }

    @Test
    @DisplayName("댓글 알림 SSE 전송 - 성공")
    void shouldSendCommentNotification_WhenValidInput() {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트사용자";
        Long postId = 123L;
        String expectedMessage = commenterName + "님이 댓글을 남겼습니다!";
        String expectedUrl = "/post/123";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendCommentNotification(postUserId, commenterName, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(postUserId, expectedEventDTO);
        verifyNoMoreInteractions(urlGenerator, notificationUtilPort, notificationSender);
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 SSE 전송 - 성공")
    void shouldSendPaperPlantNotification_WhenValidInput() {
        // Given
        Long farmOwnerId = 1L;
        String userName = "testuser";
        String expectedMessage = "롤링페이퍼에 메시지가 작성되었어요!";
        String expectedUrl = "/rolling-paper/testuser";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePaperUrl(userName)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.PAPER, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendPaperPlantNotification(farmOwnerId, userName);

        // Then
        verify(urlGenerator, times(1)).generatePaperUrl(userName);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.PAPER, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(farmOwnerId, expectedEventDTO);
        verifyNoMoreInteractions(urlGenerator, notificationUtilPort, notificationSender);
    }

    @Test
    @DisplayName("인기글 등극 알림 SSE 전송 - 성공")
    void shouldSendPostFeaturedNotification_WhenValidInput() {
        // Given
        Long userId = 1L;
        String message = "축하합니다! 게시글이 인기글로 선정되었습니다.";
        Long postId = 456L;
        String expectedUrl = "/post/456";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.POST_FEATURED, message, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendPostFeaturedNotification(userId, message, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.POST_FEATURED, message, expectedUrl);
        verify(notificationSender, times(1)).send(userId, expectedEventDTO);
        verifyNoMoreInteractions(urlGenerator, notificationUtilPort, notificationSender);
    }

    @Test
    @DisplayName("댓글 알림 SSE 전송 - null 사용자명")
    void shouldSendCommentNotification_WhenNullCommenterName() {
        // Given
        Long postUserId = 1L;
        String commenterName = null;
        Long postId = 123L;
        String expectedMessage = null + "님이 댓글을 남겼습니다!";
        String expectedUrl = "/post/123";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendCommentNotification(postUserId, commenterName, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(postUserId, expectedEventDTO);
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 SSE 전송 - null 사용자명")
    void shouldSendPaperPlantNotification_WhenNullUserName() {
        // Given
        Long farmOwnerId = 1L;
        String userName = null;
        String expectedMessage = "롤링페이퍼에 메시지가 작성되었어요!";
        String expectedUrl = "/rolling-paper/null";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePaperUrl(userName)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.PAPER, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendPaperPlantNotification(farmOwnerId, userName);

        // Then
        verify(urlGenerator, times(1)).generatePaperUrl(userName);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.PAPER, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(farmOwnerId, expectedEventDTO);
    }

    @Test
    @DisplayName("인기글 등극 알림 SSE 전송 - 빈 메시지")
    void shouldSendPostFeaturedNotification_WhenEmptyMessage() {
        // Given
        Long userId = 1L;
        String message = "";
        Long postId = 456L;
        String expectedUrl = "/post/456";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.POST_FEATURED, message, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendPostFeaturedNotification(userId, message, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.POST_FEATURED, message, expectedUrl);
        verify(notificationSender, times(1)).send(userId, expectedEventDTO);
    }

    @Test
    @DisplayName("알림 구독 - null 사용자 ID")
    void shouldSubscribe_WhenUserIdIsNull() {
        // Given
        Long userId = null;
        Long tokenId = 123L;
        SseEmitter expectedEmitter = new SseEmitter();
        
        given(ssePort.subscribe(userId, tokenId)).willReturn(expectedEmitter);

        // When
        SseEmitter result = notificationSseService.subscribe(userId, tokenId);

        // Then
        assertThat(result).isEqualTo(expectedEmitter);
        verify(ssePort, times(1)).subscribe(userId, tokenId);
        verifyNoMoreInteractions(ssePort);
    }

    @Test
    @DisplayName("댓글 알림 SSE 전송 - null 게시글 ID")
    void shouldSendCommentNotification_WhenPostIdIsNull() {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트사용자";
        Long postId = null;
        String expectedMessage = commenterName + "님이 댓글을 남겼습니다!";
        String expectedUrl = "/post/null";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendCommentNotification(postUserId, commenterName, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(postUserId, expectedEventDTO);
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 알림 SSE 전송 - 빈 사용자명")
    void shouldSendPaperPlantNotification_WhenUserNameIsEmpty() {
        // Given
        Long farmOwnerId = 1L;
        String userName = "";
        String expectedMessage = "롤링페이퍼에 메시지가 작성되었어요!";
        String expectedUrl = "/rolling-paper/";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePaperUrl(userName)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.PAPER, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendPaperPlantNotification(farmOwnerId, userName);

        // Then
        verify(urlGenerator, times(1)).generatePaperUrl(userName);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.PAPER, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(farmOwnerId, expectedEventDTO);
    }

    @Test
    @DisplayName("인기글 등극 알림 SSE 전송 - null 메시지")
    void shouldSendPostFeaturedNotification_WhenMessageIsNull() {
        // Given
        Long userId = 1L;
        String message = null;
        Long postId = 456L;
        String expectedUrl = "/post/456";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.POST_FEATURED, message, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendPostFeaturedNotification(userId, message, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.POST_FEATURED, message, expectedUrl);
        verify(notificationSender, times(1)).send(userId, expectedEventDTO);
    }

    @Test
    @DisplayName("알림 전송 - null 사용자 ID")
    void shouldSendNotification_WhenUserIdIsNull() {
        // Given
        Long userId = null;
        String commenterName = "테스트사용자";
        Long postId = 123L;
        String expectedMessage = commenterName + "님이 댓글을 남겼습니다!";
        String expectedUrl = "/post/123";
        EventDTO expectedEventDTO = new EventDTO();
        
        given(urlGenerator.generatePostUrl(postId)).willReturn(expectedUrl);
        given(notificationUtilPort.createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl))
                .willReturn(expectedEventDTO);

        // When
        notificationSseService.sendCommentNotification(userId, commenterName, postId);

        // Then
        verify(urlGenerator, times(1)).generatePostUrl(postId);
        verify(notificationUtilPort, times(1))
                .createEventDTO(NotificationType.COMMENT, expectedMessage, expectedUrl);
        verify(notificationSender, times(1)).send(userId, expectedEventDTO);
    }
}