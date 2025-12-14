package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.out.NotificationToMemberAdapter;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h2>FcmPushService 단위 테스트</h2>
 * <p>FCM 알림 전송 위임 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FcmPushServiceTest {

    @Mock private FcmAdapter fcmAdapter;
    @Mock private NotificationToMemberAdapter notificationToMemberAdapter;

    @InjectMocks private FcmPushService fcmPushService;

    @Test
    @DisplayName("댓글 알림 전송 - 토큰 있음")
    void shouldSendCommentNotificationWhenTokensPresent() throws IOException {
        List<String> tokens = List.of("token-1", "token-2");
        when(notificationToMemberAdapter.fcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(tokens);

        fcmPushService.sendNotification(NotificationType.COMMENT, 1L, "commenter", null);

        verify(notificationToMemberAdapter).fcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmAdapter, times(2)).sendMessageTo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("댓글 알림 전송 - 토큰 없음")
    void shouldSkipCommentNotificationWhenNoTokens() throws IOException {
        when(notificationToMemberAdapter.fcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(Collections.emptyList());

        fcmPushService.sendNotification(NotificationType.COMMENT, 1L, "commenter", null);

        verify(notificationToMemberAdapter).fcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmAdapter, never()).sendMessageTo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FCM 전송 실패 시 로깅 후 계속 진행")
    void shouldLogWhenSendFails() throws IOException {
        when(notificationToMemberAdapter.fcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(List.of("token-1"));
        doThrow(new IOException("fail"))
                .when(fcmAdapter).sendMessageTo(anyString(), anyString(), anyString());

        fcmPushService.sendNotification(NotificationType.COMMENT, 1L, "commenter", null);

        verify(notificationToMemberAdapter).fcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmAdapter).sendMessageTo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("인기글 알림 전송 시 메시지 내용 구성 검증")
    void shouldBuildMessageWithTitleAndBody() throws IOException {
        when(notificationToMemberAdapter.fcmEligibleFcmTokens(1L, NotificationType.POST_FEATURED_WEEKLY)).thenReturn(List.of("token-1"));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        fcmPushService.sendNotification(NotificationType.POST_FEATURED_WEEKLY, 1L, null, "게시글 제목");

        verify(fcmAdapter).sendMessageTo(tokenCaptor.capture(), titleCaptor.capture(), bodyCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo("token-1");
        assertThat(titleCaptor.getValue()).isEqualTo("축하합니다! 주간 인기글에 선정되었습니다!");
        assertThat(bodyCaptor.getValue()).contains("게시글 제목");
    }

    @Test
    @DisplayName("메시지 알림 전송")
    void shouldSendMessageNotification() throws IOException {
        List<String> tokens = List.of("token-1");
        when(notificationToMemberAdapter.fcmEligibleFcmTokens(1L, NotificationType.MESSAGE)).thenReturn(tokens);

        fcmPushService.sendNotification(NotificationType.MESSAGE, 1L, "sender", null);

        verify(notificationToMemberAdapter).fcmEligibleFcmTokens(1L, NotificationType.MESSAGE);
        verify(fcmAdapter).sendMessageTo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("친구 요청 알림 전송")
    void shouldSendFriendNotification() throws IOException {
        List<String> tokens = List.of("token-1");
        when(notificationToMemberAdapter.fcmEligibleFcmTokens(1L, NotificationType.FRIEND)).thenReturn(tokens);

        fcmPushService.sendNotification(NotificationType.FRIEND, 1L, "friend", null);

        verify(notificationToMemberAdapter).fcmEligibleFcmTokens(1L, NotificationType.FRIEND);
        verify(fcmAdapter).sendMessageTo(anyString(), anyString(), anyString());
    }
}
