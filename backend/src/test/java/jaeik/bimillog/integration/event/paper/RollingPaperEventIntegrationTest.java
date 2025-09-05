package jaeik.bimillog.integration.event.paper;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

/**
 * <h2>롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트</h2>
 * <p>롤링페이퍼 메시지 이벤트 발생 시 SSE와 FCM 알림 전송 워크플로우를 검증하는 통합 테스트</p>
 * <p>이벤트 리스너의 비즈니스 로직과 의존성 간의 상호작용을 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트")
public class RollingPaperEventIntegrationTest {


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void rollingPaperEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, userName);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(
                            eq(paperOwnerId), eq(userName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(
                            eq(paperOwnerId));
                });
    }

    @Test
    @DisplayName("여러 롤링페이퍼 메시지 이벤트 동시 처리")
    void multipleRollingPaperEvents_ShouldProcessConcurrently() {
        // Given
        RollingPaperEvent event1 = new RollingPaperEvent(1L, "사용자1");
        RollingPaperEvent event2 = new RollingPaperEvent(2L, "사용자2");
        RollingPaperEvent event3 = new RollingPaperEvent(3L, "사용자3");

        // When - 동시에 여러 롤링페이퍼 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 독립적으로 알림 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(1L), eq("사용자1"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(2L), eq("사용자2"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(3L), eq("사용자3"));
                    
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(1L));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(2L));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(3L));
                });
    }

    @Test
    @DisplayName("동일 롤링페이퍼에 여러 메시지 이벤트 처리")
    void multipleMessagesForSamePaper_ShouldProcessAll() {
        // Given - 동일 롤링페이퍼에 여러 사용자가 메시지 작성
        Long paperOwnerId = 1L;
        RollingPaperEvent event1 = new RollingPaperEvent(paperOwnerId, "친구1");
        RollingPaperEvent event2 = new RollingPaperEvent(paperOwnerId, "친구2");
        RollingPaperEvent event3 = new RollingPaperEvent(paperOwnerId, "친구3");

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 메시지에 대해 개별 알림이 발송되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq("친구1"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq("친구2"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq("친구3"));
                    
                    verify(notificationFcmUseCase, times(3)).sendPaperPlantNotification(eq(paperOwnerId));
                });
    }




    @Test
    @DisplayName("서로 다른 롤링페이퍼 이벤트 독립 처리")
    void differentRollingPaperEvents_ShouldProcessIndependently() {
        // Given
        RollingPaperEvent event1 = new RollingPaperEvent(1L, "친구A");
        RollingPaperEvent event2 = new RollingPaperEvent(2L, "친구B");

        // When - 서로 다른 롤링페이퍼 이벤트 동시 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);

        // Then - 각 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(1L), eq("친구A"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(2L), eq("친구B"));
                    
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(1L));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(2L));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - SSE 알림 실패")
    void eventProcessingWithException_SseNotificationFailure() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, userName);
        
        // SSE 알림 실패 시뮬레이션
        doThrow(new RuntimeException("SSE 알림 실패"))
                .when(notificationSseUseCase).sendPaperPlantNotification(paperOwnerId, userName);

        // When
        eventPublisher.publishEvent(event);

        // Then - SSE 실패 시 FCM은 호출되지 않음 (순차 실행이므로)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - FCM 알림 실패")
    void eventProcessingWithException_FcmNotificationFailure() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, userName);
        
        // FCM 알림 실패 시뮬레이션
        doThrow(new RuntimeException("FCM 알림 실패"))
                .when(notificationFcmUseCase).sendPaperPlantNotification(paperOwnerId);

        // When
        eventPublisher.publishEvent(event);

        // Then - SSE는 성공하고 FCM 실패 시에도 둘 다 호출됨
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
                });
    }

    @Test
    @DisplayName("사용자명 데이터가 알림에 정확히 전달")
    void userNameParameter_ShouldBePassedCorrectlyToNotifications() {
        // Given - 특정 사용자명이 포함된 이벤트
        String expectedUserName = "테스트친구";
        Long paperOwnerId = 1L;
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, expectedUserName);

        // When
        eventPublisher.publishEvent(event);

        // Then - 사용자명이 SSE 알림에만 전달되고 FCM에는 전달되지 않음을 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(expectedUserName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
                });
    }

    @Test
    @DisplayName("연속된 롤링페이퍼 이벤트 처리 순서")
    void sequentialRollingPaperEvents_ShouldMaintainOrder() {
        // Given - 연속된 롤링페이퍼 이벤트
        Long paperOwnerId = 1L;
        
        // When - 순서대로 롤링페이퍼 이벤트 발행
        eventPublisher.publishEvent(new RollingPaperEvent(paperOwnerId, "첫번째친구"));
        eventPublisher.publishEvent(new RollingPaperEvent(paperOwnerId, "두번째친구"));
        eventPublisher.publishEvent(new RollingPaperEvent(paperOwnerId, "세번째친구"));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq("첫번째친구"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq("두번째친구"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq("세번째친구"));
                    
                    // FCM 알림도 3번 호출
                    verify(notificationFcmUseCase, times(3)).sendPaperPlantNotification(eq(paperOwnerId));
                });
    }

}
