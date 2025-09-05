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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

/**
 * <h2>롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트</h2>
 * <p>롤링페이퍼에 메시지 작성 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
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
    @DisplayName("롤링페이퍼 이벤트 처리 성능 검증")
    void rollingPaperEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long paperOwnerId = 999L;
        String userName = "성능테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, userName);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "롤링페이퍼 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - null paperOwnerId")
    void rollingPaperEventCreation_ShouldValidateNullPaperOwnerId() {
        // When & Then - null paperOwnerId로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new RollingPaperEvent(null, "사용자"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("롤링페이퍼 주인 ID는 null일 수 없습니다.");
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - null userName")
    void rollingPaperEventCreation_ShouldValidateNullUserName() {
        // When & Then - null userName으로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new RollingPaperEvent(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 이름은 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("이벤트 생성 시 유효성 검증 - 빈 userName")
    void rollingPaperEventCreation_ShouldValidateEmptyUserName() {
        // When & Then - 빈 userName으로 이벤트 생성 시 예외 발생
        assertThatThrownBy(() -> new RollingPaperEvent(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 이름은 null이거나 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("대량 롤링페이퍼 메시지 이벤트 처리 성능")
    void massRollingPaperEvents_ShouldProcessEfficiently() {
        // Given - 대량의 롤링페이퍼 메시지 이벤트
        int eventCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 롤링페이퍼 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            RollingPaperEvent event = new RollingPaperEvent((long) i, "사용자" + i);
            eventPublisher.publishEvent(event);
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(notificationSseUseCase).sendPaperPlantNotification(eq((long) i), eq("사용자" + i));
                        verify(notificationFcmUseCase).sendPaperPlantNotification(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 15초를 초과하지 않아야 함
                    assert totalProcessingTime < 15000L : "대량 롤링페이퍼 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("롤링페이퍼 이벤트와 다른 이벤트의 독립적 처리")
    void rollingPaperEventWithOtherEvents_ShouldProcessIndependently() {
        // Given
        RollingPaperEvent event1 = new RollingPaperEvent(1L, "친구A");
        RollingPaperEvent event2 = new RollingPaperEvent(2L, "친구B");

        // When - 롤링페이퍼 이벤트와 다른 종류의 이벤트를 동시에 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);

        // Then - 모든 롤링페이퍼 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
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

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
                    // SSE 실패해도 FCM은 시도되어야 함
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
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

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
                });
    }

    @Test
    @DisplayName("다양한 사용자명 형태의 롤링페이퍼 이벤트 처리")
    void rollingPaperEventWithVariousUserNames_ShouldProcessCorrectly() {
        // Given - 다양한 형태의 사용자명들
        RollingPaperEvent event1 = new RollingPaperEvent(1L, "김철수");
        RollingPaperEvent event2 = new RollingPaperEvent(2L, "Anonymous123");
        RollingPaperEvent event3 = new RollingPaperEvent(3L, "친구♥");
        RollingPaperEvent event4 = new RollingPaperEvent(4L, "Best Friend Forever");

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);
        eventPublisher.publishEvent(event4);

        // Then - 모든 다양한 사용자명이 정확히 전달되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(1L), eq("김철수"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(2L), eq("Anonymous123"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(3L), eq("친구♥"));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(4L), eq("Best Friend Forever"));
                    
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(1L));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(2L));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(3L));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(4L));
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

    @Test
    @DisplayName("인기 롤링페이퍼의 대량 메시지 이벤트 처리")
    void popularPaperMassiveMessages_ShouldProcessAllEvents() {
        // Given - 인기 롤링페이퍼에 대한 대량 메시지
        Long popularPaperId = 100L;
        int messageCount = 30;
        
        long startTime = System.currentTimeMillis();

        // When - 동일 롤링페이퍼에 대한 대량 메시지 이벤트 발행
        for (int i = 1; i <= messageCount; i++) {
            eventPublisher.publishEvent(new RollingPaperEvent(popularPaperId, "친구" + i));
        }

        // Then - 모든 메시지 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    for (int i = 1; i <= messageCount; i++) {
                        verify(notificationSseUseCase).sendPaperPlantNotification(eq(popularPaperId), eq("친구" + i));
                    }
                    verify(notificationFcmUseCase, times(messageCount)).sendPaperPlantNotification(eq(popularPaperId));

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 15초를 초과하지 않아야 함
                    assert totalProcessingTime < 15000L : "인기 롤링페이퍼 대량 메시지 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }
}
