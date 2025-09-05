package jaeik.bimillog.integration.event.auth;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.auth.event.UserSignedUpEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.port.out.CommentCommandPort;
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
import static org.mockito.Mockito.*;

/**
 * <h2>사용자 도메인 이벤트 워크플로우 통합 테스트</h2>
 * <p>사용자 관련 이벤트들의 전체 흐름을 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("사용자 도메인 이벤트 워크플로우 통합 테스트")
class UserWithdrawnEventIntegrationTest {

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
    private CommentCommandPort commentCommandPort;


    @Test
    @DisplayName("사용자 차단 이벤트 워크플로우 - 소셜 로그인 해제까지 완료")
    void userBannedEventWorkflow_ShouldCompleteSocialUnlink() {
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
                    verify(socialLoginPort).unlink(eq(provider), eq(socialId));
                });
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - 댓글 익명화까지 완료")
    void userWithdrawnEventWorkflow_ShouldCompleteCommentAnonymization() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(userId));
                });
    }


    @Test
    @DisplayName("사용자 가입 이벤트 - 현재는 리스너가 없어서 처리되지 않음")
    void userSignedUpEvent_NoListenerExists() {
        // Given
        Long userId = 1L;
        UserSignedUpEvent event = new UserSignedUpEvent(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 현재 이 이벤트에 대한 리스너가 없으므로 상호작용 없음
        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verifyNoInteractions(socialLoginPort);
                    verifyNoInteractions(commentCommandPort);
                });
    }

    @Test
    @DisplayName("복합 이벤트 시나리오 - 사용자 차단 후 탈퇴")
    void complexEventScenario_UserBannedThenWithdrawn() {
        // Given
        Long userId = 1L;
        String socialId = "complexTestId";
        SocialProvider provider = SocialProvider.KAKAO;

        // When - 연속된 이벤트 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 사용자 차단으로 인한 소셜 로그인 해제
                    verify(socialLoginPort).unlink(eq(provider), eq(socialId));
                    
                    // 사용자 탈퇴로 인한 댓글 익명화
                    verify(commentCommandPort).anonymizeUserComments(eq(userId));
                });
    }

    @Test
    @DisplayName("여러 사용자 차단 이벤트 동시 처리")
    void multipleUserBannedEvents() {
        // Given
        UserBannedEvent event1 = new UserBannedEvent(1L, "kakao123", SocialProvider.KAKAO);
        UserBannedEvent event2 = new UserBannedEvent(2L, "kakao456", SocialProvider.KAKAO);
        UserBannedEvent event3 = new UserBannedEvent(3L, "kakao789", SocialProvider.KAKAO);

        // When - 여러 사용자 차단 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 사용자의 소셜 로그인이 해제되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakao123"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakao456"));
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("kakao789"));
                });
    }

    @Test
    @DisplayName("여러 사용자 탈퇴 이벤트 동시 처리")
    void multipleUserWithdrawnEvents() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 여러 사용자 탈퇴 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId1));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId3));

        // Then - 모든 사용자의 댓글이 익명화되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(userId1));
                    verify(commentCommandPort).anonymizeUserComments(eq(userId2));
                    verify(commentCommandPort).anonymizeUserComments(eq(userId3));
                });
    }


    @Test
    @DisplayName("동일 사용자의 여러 이벤트 처리")
    void multipleEventsForSameUser() {
        // Given
        Long userId = 100L;
        String socialId = "sameUserTest";

        // When - 동일 사용자에 대해 여러 이벤트 연속 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));

        // Then - 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 사용자 차단
                    verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq(socialId));
                    
                    // 탈퇴
                    verify(commentCommandPort).anonymizeUserComments(eq(userId));
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 사용자 차단")
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

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 사용자 탈퇴")
    void userWithdrawnEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(1L);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(1L));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("null 값을 포함한 사용자 이벤트 처리")
    void userEventsWithNullValues_ShouldBeProcessed() {
        // Given - null 값들을 포함한 이벤트들
        UserBannedEvent bannedEvent = new UserBannedEvent(null, null, null);
        UserWithdrawnEvent withdrawnEvent = new UserWithdrawnEvent(null);

        // When
        eventPublisher.publishEvent(bannedEvent);
        eventPublisher.publishEvent(withdrawnEvent);

        // Then - null 값이어도 이벤트는 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialLoginPort).unlink(eq(null), eq(null));
                    verify(commentCommandPort).anonymizeUserComments(eq(null));
                });
    }

    @Test
    @DisplayName("대용량 이벤트 처리 - 100명 사용자 동시 차단")
    void bulkUserBannedEvents_ShouldCompleteAllWithinTimeout() {
        // Given - 100명의 사용자 차단 이벤트
        int eventCount = 100;

        // When - 대량 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new UserBannedEvent(
                    (long) i, "bulkTest" + i, SocialProvider.KAKAO));
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    // 모든 사용자에 대해 소셜 로그인 해제가 호출되었는지 확인
                    for (int i = 1; i <= eventCount; i++) {
                        verify(socialLoginPort).unlink(eq(SocialProvider.KAKAO), eq("bulkTest" + i));
                    }
                });
    }

    @Test
    @DisplayName("이벤트 발행 순서와 처리 순서 - 비동기 특성 확인")
    void eventOrderAndAsyncProcessing() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 순서대로 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId1));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId3));

        // Then - 비동기 처리이므로 순서와 관계없이 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(userId1));
                    verify(commentCommandPort).anonymizeUserComments(eq(userId2));
                    verify(commentCommandPort).anonymizeUserComments(eq(userId3));
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
                });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 댓글 익명화 실패")
    void eventProcessingWithException_CommentAnonymizationFailure() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(1L);
        
        // 댓글 익명화 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 익명화 실패"))
                .when(commentCommandPort).anonymizeUserComments(1L);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(1L));
                });
    }
}