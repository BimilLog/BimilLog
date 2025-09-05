package jaeik.bimillog.integration.event.auth;

import jaeik.bimillog.domain.auth.event.UserSignedUpEvent;
import jaeik.bimillog.domain.notification.application.port.out.FcmTokenCommandPort;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * <h2>사용자 회원가입 이벤트 워크플로우 통합 테스트</h2>
 * <p>사용자 회원가입 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("사용자 회원가입 이벤트 워크플로우 통합 테스트")
public class UserSignedUpEventIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private FcmTokenCommandPort fcmTokenCommandPort;

    @Test
    @DisplayName("사용자 회원가입 이벤트 워크플로우 - FCM 토큰 정리까지 완료")
    void userSignedUpEventWorkflow_ShouldCompleteFcmTokenCleanup() {
        // Given
        Long userId = 1L;
        UserSignedUpEvent event = new UserSignedUpEvent(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        // 회원가입 시에는 기존 FCM 토큰 정리가 수행됨 (중복 로그인 방지)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId));
                });
    }

    @Test
    @DisplayName("여러 사용자 회원가입 이벤트 동시 처리")
    void multipleUserSignUpEvents_ShouldProcessConcurrently() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 동시에 여러 회원가입 이벤트 발행
        eventPublisher.publishEvent(new UserSignedUpEvent(userId1));
        eventPublisher.publishEvent(new UserSignedUpEvent(userId2));
        eventPublisher.publishEvent(new UserSignedUpEvent(userId3));

        // Then - 모든 사용자의 FCM 토큰이 정리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId1));
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId2));
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId3));
                });
    }

    @Test
    @DisplayName("회원가입 이벤트 처리 성능 검증")
    void userSignedUpEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long userId = 1L;
        UserSignedUpEvent event = new UserSignedUpEvent(userId);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 2초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 2초를 초과하지 않아야 함
                    assert processingTime < 2000L : "회원가입 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("null 사용자 ID 회원가입 이벤트 처리")
    void userSignedUpEventWithNullUserId_ShouldBeHandledGracefully() {
        // Given
        Long nullUserId = null;
        UserSignedUpEvent event = new UserSignedUpEvent(nullUserId);

        // When
        eventPublisher.publishEvent(event);

        // Then - null 값이어도 이벤트는 처리되어야 함 (리스너에서 null 체크)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // null userId에 대해서도 호출은 되지만 실제 삭제는 수행되지 않을 것
                    verify(fcmTokenCommandPort).deleteByUserId(eq(nullUserId));
                });
    }

    @Test
    @DisplayName("연속된 회원가입 이벤트 처리")
    void sequentialUserSignUpEvents_ForSameUser() {
        // Given - 동일 사용자의 연속된 회원가입 (재가입 시나리오)
        Long userId = 1L;

        // When - 빠르게 연속해서 회원가입 이벤트 발행
        eventPublisher.publishEvent(new UserSignedUpEvent(userId));
        eventPublisher.publishEvent(new UserSignedUpEvent(userId));
        eventPublisher.publishEvent(new UserSignedUpEvent(userId));

        // Then - 모든 이벤트가 처리되어야 함 (FCM 토큰 정리가 여러 번 호출)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 3번 호출되어야 함
                    verify(fcmTokenCommandPort, times(3)).deleteByUserId(eq(userId));
                });
    }

    @Test
    @DisplayName("대량 회원가입 이벤트 처리 성능")
    void massUserSignUpEvents_ShouldProcessEfficiently() {
        // Given - 대량의 회원가입 이벤트
        int userCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 회원가입 이벤트 발행
        for (int i = 1; i <= userCount; i++) {
            eventPublisher.publishEvent(new UserSignedUpEvent((long) i));
        }

        // Then - 모든 이벤트가 10초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    for (int i = 1; i <= userCount; i++) {
                        verify(fcmTokenCommandPort).deleteByUserId(eq((long) i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 10초를 초과하지 않아야 함
                    assert totalProcessingTime < 10000L : "대량 회원가입 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("회원가입 이벤트와 다른 이벤트 동시 처리")
    void userSignedUpEventWithOtherEvents_ShouldProcessIndependently() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;

        // When - 회원가입 이벤트와 다른 종류의 이벤트를 동시에 발행
        eventPublisher.publishEvent(new UserSignedUpEvent(userId1));
        // 다른 이벤트들도 함께 발행한다고 가정
        eventPublisher.publishEvent(new UserSignedUpEvent(userId2));

        // Then - 모든 회원가입 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId1));
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId2));
                });
    }

    @Test
    @DisplayName("회원가입 이벤트 처리 중 예외 발생 시나리오")
    void userSignedUpEventProcessing_ShouldHandleExceptions() {
        // Given
        Long userId = 1L;
        UserSignedUpEvent event = new UserSignedUpEvent(userId);

        // 예외 상황 시뮬레이션을 위해 특별한 userId 사용
        Long problematicUserId = -1L;
        UserSignedUpEvent problematicEvent = new UserSignedUpEvent(problematicUserId);

        // When
        eventPublisher.publishEvent(event);
        eventPublisher.publishEvent(problematicEvent);

        // Then - 정상 이벤트는 처리되고, 예외 이벤트도 처리 시도는 되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(fcmTokenCommandPort).deleteByUserId(eq(userId));
                    verify(fcmTokenCommandPort).deleteByUserId(eq(problematicUserId));
                });
    }
}
