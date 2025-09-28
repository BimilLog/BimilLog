package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.notification.application.port.out.UrlGeneratorPort;
import jaeik.bimillog.domain.notification.application.service.SseService;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>SseService 테스트</h2>
 * <p>SSE 알림 서비스의 메시지 구성 및 포트 위임을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SseService 테스트")
@Tag("test")
class SseServiceTest {

    @Mock
    private SsePort ssePort;

    @Mock
    private UrlGeneratorPort urlGeneratorPort;

    @InjectMocks
    private SseService notificationSseService;

    @Test
    @DisplayName("SSE 구독은 포트에서 생성한 Emitter를 그대로 반환한다")
    void shouldReturnEmitterFromPort() {
        // Given
        Long userId = 1L;
        Long tokenId = 2L;
        SseEmitter emitter = new SseEmitter(1000L);
        given(ssePort.subscribe(userId, tokenId)).willReturn(emitter);

        // When
        SseEmitter result = notificationSseService.subscribe(userId, tokenId);

        // Then
        assertThat(result).isEqualTo(emitter);
        verify(ssePort).subscribe(userId, tokenId);
    }

    @Test
    @DisplayName("사용자 탈퇴 시 모든 Emitter를 제거한다")
    void shouldDeleteAllEmittersByUserId() {
        // Given
        Long userId = 10L;

        // When
        notificationSseService.deleteEmitters(userId, null);

        // Then
        verify(ssePort).deleteEmitters(userId, null);
    }

    @Test
    @DisplayName("특정 기기 로그아웃 시 해당 Emitter만 제거한다")
    void shouldDeleteEmitterByUserAndToken() {
        // Given
        Long userId = 10L;
        Long tokenId = 99L;

        // When
        notificationSseService.deleteEmitters(userId, tokenId);

        // Then
        verify(ssePort).deleteEmitters(userId, tokenId);
    }

    @Test
    @DisplayName("댓글 알림 SSE 메시지를 보낸다")
    void shouldSendCommentNotification() {
        // Given
        Long postUserId = 50L;
        Long postId = 77L;
        String commenterName = "댓글러";
        String expectedUrl = "/posts/" + postId;
        given(urlGeneratorPort.generatePostUrl(postId)).willReturn(expectedUrl);

        // When
        notificationSseService.sendCommentNotification(postUserId, commenterName, postId);

        // Then
        verify(urlGeneratorPort).generatePostUrl(postId);
        verify(ssePort).send(argThat(message ->
                matchesMessage(message, postUserId, NotificationType.COMMENT,
                        commenterName + "님이 댓글을 남겼습니다!", expectedUrl)
        ));
    }

    @Test
    @DisplayName("롤링페이퍼 알림 SSE 메시지를 보낸다")
    void shouldSendPaperNotification() {
        // Given
        Long farmOwnerId = 99L;
        String userName = "롤링페이퍼";
        String expectedUrl = "/paper/" + userName;
        given(urlGeneratorPort.generateRollingPaperUrl(userName)).willReturn(expectedUrl);

        // When
        notificationSseService.sendPaperPlantNotification(farmOwnerId, userName);

        // Then
        verify(urlGeneratorPort).generateRollingPaperUrl(userName);
        verify(ssePort).send(argThat(message ->
                matchesMessage(message, farmOwnerId, NotificationType.PAPER,
                        "롤링페이퍼에 메시지가 작성되었어요!", expectedUrl)
        ));
    }

    @Test
    @DisplayName("인기글 알림 SSE 메시지를 보낸다")
    void shouldSendPostFeaturedNotification() {
        // Given
        Long userId = 7L;
        Long postId = 31L;
        String message = "인기글 축하";
        String expectedUrl = "/posts/" + postId;
        given(urlGeneratorPort.generatePostUrl(postId)).willReturn(expectedUrl);

        // When
        notificationSseService.sendPostFeaturedNotification(userId, message, postId);

        // Then
        verify(urlGeneratorPort).generatePostUrl(postId);
        verify(ssePort).send(argThat(sseMessage ->
                matchesMessage(sseMessage, userId, NotificationType.POST_FEATURED, message, expectedUrl)
        ));
    }

    private boolean matchesMessage(SseMessage message, Long userId, NotificationType type, String expectedMessage, String expectedUrl) {
        return message != null
                && message.userId().equals(userId)
                && message.type() == type
                && message.message().equals(expectedMessage)
                && message.url().equals(expectedUrl);
    }
}
