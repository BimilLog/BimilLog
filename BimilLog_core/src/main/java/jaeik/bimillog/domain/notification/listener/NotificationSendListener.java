package jaeik.bimillog.domain.notification.listener;

import jaeik.bimillog.domain.notification.event.AlarmSendEvent;
import jaeik.bimillog.domain.notification.service.FcmPushService;
import jaeik.bimillog.domain.notification.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>알림 생성 이벤트 리스너</h2>
 * <p>다양한 도메인에서 발행하는 비즈니스 이벤트를 수신합니다.</p>
 * <p>댓글 작성, 롤링페이퍼 작성, 인기글 선정 이벤트 처리</p>
 * <p>SSE 실시간 알림과 FCM 푸시 알림을 병렬 전송</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "알림 전송 이벤트")
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSendListener {
    private final SseService sseService;
    private final FcmPushService fcmPushService;

    /**
     * <h3>SSE 실시간 알림 전송</h3>
     * <p>AlarmSendEvent를 수신하여 SSE 알림을 전송합니다.</p>
     *
     * @param event 알림 전송 이벤트
     */
    @Async("sseNotificationExecutor")
    @TransactionalEventListener(AlarmSendEvent.class)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void sendSSENotification(AlarmSendEvent event) {
        sseService.sendNotification(
                event.memberId(),
                event.type(),
                event.message(),
                event.url()
        );
    }

    /**
     * <h3>FCM 푸시 알림 전송</h3>
     * <p>AlarmSendEvent를 수신하여 FCM 알림을 전송합니다.</p>
     * <p>FCM 메시지 조립은 FcmCommandService에서 처리합니다.</p>
     *
     * @param event 알림 전송 이벤트
     */
    @Async("fcmNotificationExecutor")
    @TransactionalEventListener(AlarmSendEvent.class)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void sendFCMNotification(AlarmSendEvent event) {
        fcmPushService.sendNotification(
                event.type(),
                event.memberId(),
                event.relatedMemberName(),
                event.postTitle());
    }
}
