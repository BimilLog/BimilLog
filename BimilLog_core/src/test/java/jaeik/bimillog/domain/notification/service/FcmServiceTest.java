package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.testutil.TestMembers;
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
 * <p>FCM 토큰 삭제와 알림 전송 위임 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FcmServiceTest {

    @Mock private FcmPort fcmPort;
    @Mock private NotificationUtilPort notificationUtilPort;

    @InjectMocks private FcmService fcmService;

    @Test
    @DisplayName("토큰 삭제 - 특정 토큰 ID")
    void shouldDeleteSpecificToken() {
        fcmService.deleteFcmTokens(1L, 10L);
        verify(fcmPort).deleteFcmTokens(1L, 10L);
    }

    @Test
    @DisplayName("토큰 삭제 - 전체 토큰")
    void shouldDeleteAllTokens() {
        fcmService.deleteFcmTokens(1L, null);
        verify(fcmPort).deleteFcmTokens(1L, null);
    }

    @Test
    @DisplayName("댓글 알림 전송 - 토큰 있음")
    void shouldSendCommentNotificationWhenTokensPresent() throws IOException {
        List<FcmToken> tokens = List.of(
                FcmToken.create(TestMembers.MEMBER_1, "token-1"),
                FcmToken.create(TestMembers.MEMBER_1, "token-2")
        );
        when(notificationUtilPort.FcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(tokens);

        fcmService.sendCommentNotification(1L, "commenter");

        verify(notificationUtilPort).FcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmPort, times(2)).sendMessageTo(any(FcmMessage.class));
    }

    @Test
    @DisplayName("댓글 알림 전송 - 토큰 없음")
    void shouldSkipCommentNotificationWhenNoTokens() throws IOException {
        when(notificationUtilPort.FcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(Collections.emptyList());

        fcmService.sendCommentNotification(1L, "commenter");

        verify(notificationUtilPort).FcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmPort, never()).sendMessageTo(any());
    }

    @Test
    @DisplayName("FCM 전송 실패 시 로깅 후 계속 진행")
    void shouldLogWhenSendFails() throws IOException {
        FcmToken failingToken = FcmToken.create(TestMembers.MEMBER_1, "token-1");
        when(notificationUtilPort.FcmEligibleFcmTokens(1L, NotificationType.COMMENT)).thenReturn(List.of(failingToken));
        doThrow(new IOException("fail"))
                .when(fcmPort).sendMessageTo(any(FcmMessage.class));

        fcmService.sendCommentNotification(1L, "commenter");

        verify(notificationUtilPort).FcmEligibleFcmTokens(1L, NotificationType.COMMENT);
        verify(fcmPort).sendMessageTo(any(FcmMessage.class));
    }

    @Test
    @DisplayName("알림 전송 시 메시지 내용 구성 검증")
    void shouldBuildMessageWithTitleAndBody() throws IOException {
        FcmToken token = FcmToken.create(TestMembers.MEMBER_1, "token-1");
        when(notificationUtilPort.FcmEligibleFcmTokens(1L, NotificationType.POST_FEATURED)).thenReturn(List.of(token));

        ArgumentCaptor<FcmMessage> messageCaptor = ArgumentCaptor.forClass(FcmMessage.class);

        fcmService.sendPostFeaturedNotification(1L, "title", "body");

        verify(fcmPort).sendMessageTo(messageCaptor.capture());
        FcmMessage captured = messageCaptor.getValue();
        assertThat(captured.title()).isEqualTo("title");
        assertThat(captured.body()).isEqualTo("body");
        assertThat(captured.token()).isEqualTo("token-1");
    }
}
