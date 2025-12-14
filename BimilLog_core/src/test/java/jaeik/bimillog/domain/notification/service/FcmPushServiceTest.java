package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.infrastructure.api.fcm.FcmAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>FcmService 단위 테스트</h2>
 * <p>FCM 알림 전송 위임 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FcmPushServiceTest {

    @Mock private FcmAdapter fcmAdapter;
    @Mock private NotificationQueryRepository notificationUtilRepository;

    @InjectMocks private FcmPushService fcmPushService;

    @Test
    @DisplayName("댓글 알림 전송 - 토큰 있음")
    void shouldSendCommentNotificationWhenTokensPresent() throws IOException {
        List<String> tokens = List.of("token-1", "token-2");
        when(notificationUtilRepository.fcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(tokens);

        fcmPushService.sendCommentNotification(1L, "commenter");

        verify(notificationUtilRepository).fcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmAdapter, times(2)).sendMessageTo(any(FcmMessage.class));
    }

    @Test
    @DisplayName("댓글 알림 전송 - 토큰 없음")
    void shouldSkipCommentNotificationWhenNoTokens() throws IOException {
        when(notificationUtilRepository.fcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(Collections.emptyList());

        fcmPushService.sendCommentNotification(1L, "commenter");

        verify(notificationUtilRepository).fcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmAdapter, never()).sendMessageTo(any());
    }

    @Test
    @DisplayName("FCM 전송 실패 시 로깅 후 계속 진행")
    void shouldLogWhenSendFails() throws IOException {
        when(notificationUtilRepository.fcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(List.of("token-1"));
        doThrow(new IOException("fail"))
                .when(fcmAdapter).sendMessageTo(any(FcmMessage.class));

        fcmPushService.sendCommentNotification(1L, "commenter");

        verify(notificationUtilRepository).fcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmAdapter).sendMessageTo(any(FcmMessage.class));
    }

    @Test
    @DisplayName("알림 전송 시 메시지 내용 구성 검증")
    void shouldBuildMessageWithTitleAndBody() throws IOException {
        when(notificationUtilRepository.fcmEligibleFcmTokens(1L, NotificationType.POST_FEATURED)).thenReturn(List.of("token-1"));

        ArgumentCaptor<FcmMessage> messageCaptor = ArgumentCaptor.forClass(FcmMessage.class);

        fcmPushService.sendPostFeaturedNotification(1L, "title", "body");

        verify(fcmAdapter).sendMessageTo(messageCaptor.capture());
        FcmMessage captured = messageCaptor.getValue();
        assertThat(captured.title()).isEqualTo("title");
        assertThat(captured.body()).isEqualTo("body");
        assertThat(captured.token()).isEqualTo("token-1");
    }
}
