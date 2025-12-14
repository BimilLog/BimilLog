package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.out.SseRepository;
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
@Tag("unit")
class SseServiceTest {

    @Mock
    private SseRepository sseRepository;

    @InjectMocks
    private SseService notificationSseService;

    @Test
    @DisplayName("SSE 구독은 포트에서 생성한 Emitter를 그대로 반환한다")
    void shouldReturnEmitterFromPort() {
        // Given
        Long memberId = 1L;
        Long tokenId = 2L;
        SseEmitter emitter = new SseEmitter(1000L);
        given(sseRepository.subscribe(memberId, tokenId)).willReturn(emitter);

        // When
        SseEmitter result = notificationSseService.subscribe(memberId, tokenId);

        // Then
        assertThat(result).isEqualTo(emitter);
        verify(sseRepository).subscribe(memberId, tokenId);
    }

    @Test
    @DisplayName("사용자 탈퇴 시 모든 Emitter를 제거한다")
    void shouldDeleteAllEmittersByUserId() {
        // Given
        Long memberId = 10L;

        // When
        notificationSseService.deleteEmitters(memberId, null);

        // Then
        verify(sseRepository).deleteEmitters(memberId, null);
    }

    @Test
    @DisplayName("특정 기기 로그아웃 시 해당 Emitter만 제거한다")
    void shouldDeleteEmitterByUserAndToken() {
        // Given
        Long memberId = 10L;
        Long tokenId = 99L;

        // When
        notificationSseService.deleteEmitters(memberId, tokenId);

        // Then
        verify(sseRepository).deleteEmitters(memberId, tokenId);
    }

    @Test
    @DisplayName("댓글 알림 SSE 메시지를 보낸다")
    void shouldSendCommentNotification() {
        // Given
        Long postUserId = 50L;
        String message = "댓글러님이 댓글을 남겼습니다!";
        String url = "http://localhost:3000/board/post/77";

        // When
        notificationSseService.sendNotification(postUserId, NotificationType.COMMENT, message, url);

        // Then
        verify(sseRepository).send(argThat(sseMessage ->
                matchesMessage(sseMessage, postUserId, NotificationType.COMMENT, message, url)
        ));
    }

    @Test
    @DisplayName("롤링페이퍼 알림 SSE 메시지를 보낸다")
    void shouldSendPaperNotification() {
        // Given
        Long farmOwnerId = 99L;
        String message = "롤링페이퍼에 메시지가 작성되었어요!";
        String url = "http://localhost:3000/paper/롤링페이퍼";

        // When
        notificationSseService.sendNotification(farmOwnerId, NotificationType.MESSAGE, message, url);

        // Then
        verify(sseRepository).send(argThat(sseMessage ->
                matchesMessage(sseMessage, farmOwnerId, NotificationType.MESSAGE, message, url)
        ));
    }

    @Test
    @DisplayName("주간 인기글 알림 SSE 메시지를 보낸다")
    void shouldSendPostFeaturedWeeklyNotification() {
        // Given
        Long memberId = 7L;
        String message = "축하합니다! 주간 인기글에 선정되었습니다!";
        String url = "http://localhost:3000/board/post/31";

        // When
        notificationSseService.sendNotification(memberId, NotificationType.POST_FEATURED_WEEKLY, message, url);

        // Then
        verify(sseRepository).send(argThat(sseMessage ->
                matchesMessage(sseMessage, memberId, NotificationType.POST_FEATURED_WEEKLY, message, url)
        ));
    }

    @Test
    @DisplayName("친구 요청 알림 SSE 메시지를 보낸다")
    void shouldSendFriendNotification() {
        // Given
        Long memberId = 5L;
        String message = "새로운 친구 요청이 도착했어요!";
        String url = "http://localhost:3000/friends";

        // When
        notificationSseService.sendNotification(memberId, NotificationType.FRIEND, message, url);

        // Then
        verify(sseRepository).send(argThat(sseMessage ->
                matchesMessage(sseMessage, memberId, NotificationType.FRIEND, message, url)
        ));
    }

    private boolean matchesMessage(SseMessage message, Long memberId, NotificationType type, String expectedMessage, String expectedUrl) {
        return message != null
                && message.memberId().equals(memberId)
                && message.type() == type
                && message.message().equals(expectedMessage)
                && message.url().equals(expectedUrl);
    }
}
