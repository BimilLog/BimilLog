package jaeik.bimillog.domain.paper.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.service.FcmPushService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트</h2>
 * <p>롤링페이퍼 메시지 이벤트 발생 시 SSE와 FCM 알림 전송 워크플로우를 검증하는 통합 테스트</p>
 * <p>이벤트 리스너의 비즈니스 로직과 의존성 간의 상호작용을 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트")
@Tag("integration")
@Execution(ExecutionMode.SAME_THREAD)
public class RollingPaperEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private FcmPushService fcmPushService;

    @Test
    @DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void rollingPaperEventWorkflow_ShouldCompleteNotifications() {
        // 1. Given
        Long paperOwnerId = testMember.getId();
        String memberName = testMember.getMemberName();
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // 2. When
        eventPublisher.publishEvent(event);

        // 트랜잭션 커밋 (AFTER_COMMIT 리스너 트리거)
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }

        // 3. Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // SSE 알림 검증
            verify(sseService, times(1)).sendNotification(
                    eq(paperOwnerId),
                    eq(NotificationType.MESSAGE),
                    eq("롤링페이퍼에 메시지가 작성되었어요!"),
                    anyString());

            // FCM 알림 검증 (관련 멤버명과 포스트 타이틀은 현재 로직상 null 전송)
            verify(fcmPushService, times(1)).sendNotification(
                    eq(NotificationType.MESSAGE),
                    eq(paperOwnerId),
                    isNull(),
                    isNull());
        });
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - SSE 알림 실패 시에도 FCM은 전송되어야 함")
    void eventProcessingWithException_SseNotificationFailure() {
        // 1. Given
        Long paperOwnerId = testMember.getId();
        String memberName = testMember.getMemberName();
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // SSE 알림 실패 시뮬레이션
        doThrow(new RuntimeException("SSE 연결 오류"))
                .when(sseService).sendNotification(anyLong(), any(), anyString(), anyString());

        // 2. When
        eventPublisher.publishEvent(event);

        // 트랜잭션 커밋
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }

        // 3. Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // SSE 서비스가 호출되었으나 내부에서 예외가 발생했음을 확인
            verify(sseService, times(1)).sendNotification(
                    eq(paperOwnerId),
                    any(),
                    anyString(),
                    anyString());

            // [핵심] SSE가 실패했음에도 독립적인 스레드에서 돌아가는 FCM은 성공해야 함
            verify(fcmPushService, times(1)).sendNotification(
                    eq(NotificationType.MESSAGE),
                    eq(paperOwnerId),
                    isNull(),
                    isNull());
        });
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - FCM 알림 실패 시에도 SSE는 전송되어야 함")
    void eventProcessingWithException_FcmNotificationFailure() {
        // 1. Given (준비)
        Long paperOwnerId = testMember.getId();
        String memberName = testMember.getMemberName();
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // FCM 알림 시 예외가 발생하도록 설정
        doThrow(new RuntimeException("FCM 전송 서버 장애"))
                .when(fcmPushService).sendNotification(any(), anyLong(), any(), any());

        // 2. When (실행)
        // RollingPaperEvent 발행
        eventPublisher.publishEvent(event);

        // [핵심] TransactionPhase.AFTER_COMMIT 리스너를 트리거하기 위해 트랜잭션 강제 종료/커밋
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit(); // 커밋 상태로 마킹
            TestTransaction.end();           // 트랜잭션 종료 (이때 AFTER_COMMIT 리스너들이 동작 시작)
        }

        // 3. Then (검증)
        // 비동기(@Async)로 실행되므로 Awaitility를 사용하여 최대 5초간 대기하며 검증
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // SSE 알림 전송이 성공했는지 확인
            verify(sseService, times(1)).sendNotification(
                    eq(paperOwnerId),
                    eq(NotificationType.MESSAGE),
                    contains("롤링페이퍼"), // 메시지 내용 일부 확인
                    anyString()
            );

            // FCM 알림 서비스가 호출은 되었는지 확인 (내부적으로 예외가 터졌을 것임)
            verify(fcmPushService, times(1)).sendNotification(
                    eq(NotificationType.MESSAGE),
                    eq(paperOwnerId),
                    any(),
                    any()
            );
        });
    }

}
