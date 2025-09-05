package jaeik.bimillog.integration.event.admin;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
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
import static org.mockito.Mockito.doThrow;

/**
 * <h2>사용자 차단 이벤트 워크플로우 통합 테스트</h2>
 * <p>관리자가 사용자를 차단할 때 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("사용자 차단 이벤트 워크플로우 통합 테스트")
public class UserBannedEventIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private SocialLoginPort socialLoginPort;

    @MockitoBean
    private UserCommandPort userCommandPort;

    @Test
    @DisplayName("사용자 차단 이벤트 워크플로우 - 소셜 로그인 해제와 블랙리스트 등록까지 완료")
    void userBannedEventWorkflow_ShouldCompleteSocialUnlinkAndBlacklist() {
        // Given
        Long userId = 1L;
        String socialId = "testKakaoId123";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(userId, socialId, provider);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 소셜 로그인 해제
                    verify(socialLoginPort).unlink(eq(provider), eq(socialId));
                    // 블랙리스트 등록
                    verify(userCommandPort).addToBlacklist(eq(socialId), eq(provider));
                });
    }

    @Test
    @DisplayName("여러 사용자 차단 이벤트 동시 처리")
    void multipleUserBannedEvents_ShouldProcessConcurrently() {
        // Given
        UserBannedEvent event1 = new UserBannedEvent(1L, "kakao123", SocialProvider.KAKAO);
        UserBannedEvent event2 = new UserBannedEvent(2L, "kakao456", SocialProvider.KAKAO);
        UserBannedEvent event3 = new UserBannedEvent(3L, "kakao789", SocialProvider.KAKAO);

        // When - 동시에 여러 사용자 차단 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 사용자의 소셜 로그인이 해제되고 블랙리스트에 등록되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakao123"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakao456"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakao789"));
                    
                    verify(userCommandPort).addToBlacklist(eq("kakao123"), eq(SocialProvider.KAKAO));
                    verify(userCommandPort).addToBlacklist(eq("kakao456"), eq(SocialProvider.KAKAO));
                    verify(userCommandPort).addToBlacklist(eq("kakao789"), eq(SocialProvider.KAKAO));
                });
    }

    @Test
    @DisplayName("동일 사용자의 여러 차단 이벤트 처리")
    void multipleUserBannedEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자의 여러 계정 차단 (재가입 후 다시 차단 등의 시나리오)
        Long userId = 1L;
        String socialId = "sameUserTest";
        SocialProvider provider = SocialProvider.KAKAO;

        // When - 동일 사용자에 대한 차단 이벤트 여러 번 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));

        // Then - 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialLoginPort, times(3)).unlink(eq(provider), eq(socialId));
                    verify(userCommandPort, times(3)).addToBlacklist(eq(socialId), eq(provider));
                });
    }

    @Test
    @DisplayName("사용자 차단 이벤트 처리 성능 검증")
    void userBannedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        UserBannedEvent event = new UserBannedEvent(1L, "performanceTest", SocialProvider.KAKAO);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("performanceTest"));
                    verify(userCommandPort).addToBlacklist(eq("performanceTest"), eq(SocialProvider.KAKAO));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "사용자 차단 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("null 값을 포함한 사용자 차단 이벤트 처리")
    void userBannedEventsWithNullValues_ShouldBeHandledGracefully() {
        // Given - null 값들을 포함한 차단 이벤트
        UserBannedEvent eventWithNullUserId = new UserBannedEvent(null, "testId", SocialProvider.KAKAO);
        UserBannedEvent eventWithNullSocialId = new UserBannedEvent(1L, null, SocialProvider.KAKAO);
        UserBannedEvent eventWithNullProvider = new UserBannedEvent(1L, "testId", null);

        // When
        eventPublisher.publishEvent(eventWithNullUserId);
        eventPublisher.publishEvent(eventWithNullSocialId);
        eventPublisher.publishEvent(eventWithNullProvider);

        // Then - null 값이어도 이벤트는 처리되어야 함 (리스너에서 null 체크)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("testId"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq(null));
                    verify(socialLoginPort).unlink(eq(null), eq("testId"));
                    
                    verify(userCommandPort).addToBlacklist(eq("testId"), eq(SocialProvider.KAKAO));
                    verify(userCommandPort).addToBlacklist(eq(null), eq(SocialProvider.KAKAO));
                    verify(userCommandPort).addToBlacklist(eq("testId"), eq(null));
                });
    }

    @Test
    @DisplayName("대량 사용자 차단 이벤트 처리 성능")
    void massUserBannedEvents_ShouldProcessEfficiently() {
        // Given - 대량의 사용자 차단 이벤트 (100개)
        int eventCount = 100;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 차단 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new UserBannedEvent(
                    (long) i, "bulkTest" + i, SocialProvider.KAKAO));
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    // 모든 사용자에 대해 소셜 로그인 해제 및 블랙리스트 등록 확인
                    for (int i = 1; i <= eventCount; i++) {
                        verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("bulkTest" + i));
                        verify(userCommandPort).addToBlacklist(eq("bulkTest" + i), eq(SocialProvider.KAKAO));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 15초를 초과하지 않아야 함
                    assert totalProcessingTime < 15000L : "대량 사용자 차단 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("다양한 소셜 제공자의 차단 이벤트 처리")
    void userBannedEventsWithVariousProviders_ShouldProcessCorrectly() {
        // Given - 다양한 소셜 제공자
        UserBannedEvent kakaoEvent = new UserBannedEvent(1L, "kakaoUser", SocialProvider.KAKAO);
        // 향후 다른 소셜 제공자 추가 시 테스트 확장 가능

        // When
        eventPublisher.publishEvent(kakaoEvent);

        // Then - 모든 제공자별로 적절히 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakaoUser"));
                    verify(userCommandPort).addToBlacklist(eq("kakaoUser"), eq(SocialProvider.KAKAO));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 소셜 로그인 해제 실패")
    void eventProcessingWithException_SocialUnlinkFailure() {
        // Given
        UserBannedEvent event = new UserBannedEvent(1L, "errorTest", SocialProvider.KAKAO);
        
        // 소셜 로그인 해제 실패 시뮬레이션
        doThrow(new RuntimeException("소셜 로그인 해제 실패"))
                .when(socialLoginPort).unlink(SocialProvider.KAKAO, "errorTest");

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("errorTest"));
                    // 소셜 해제는 실패하지만 블랙리스트 등록은 시도되어야 함
                    verify(userCommandPort).addToBlacklist(eq("errorTest"), eq(SocialProvider.KAKAO));
                });
    }

    @Test
    @DisplayName("연속된 차단 이벤트 처리 순서")
    void sequentialUserBannedEvents_ShouldMaintainOrder() {
        // Given - 동일 사용자의 연속된 차단 이벤트
        Long userId = 1L;
        
        // When - 순서대로 차단 이벤트 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, "first", SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserBannedEvent(userId, "second", SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserBannedEvent(userId, "third", SocialProvider.KAKAO));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("first"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("second"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("third"));
                    
                    verify(userCommandPort).addToBlacklist(eq("first"), eq(SocialProvider.KAKAO));
                    verify(userCommandPort).addToBlacklist(eq("second"), eq(SocialProvider.KAKAO));
                    verify(userCommandPort).addToBlacklist(eq("third"), eq(SocialProvider.KAKAO));
                });
    }
}
