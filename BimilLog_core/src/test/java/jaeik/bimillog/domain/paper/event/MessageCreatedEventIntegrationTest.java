package jaeik.bimillog.domain.paper.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperUpdateAdapter;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>메시지 작성 이벤트 워크플로우 통합 테스트</h2>
 * <p>메시지 작성 시 발생하는 실시간 인기 점수 증가 및 알림 저장을 검증하는 통합 테스트</p>
 * <p>RollingPaperEvent 발생 시 비동기로 실시간 인기 점수가 5점 증가하고 알림이 저장되는지 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.2.0
 */
@DisplayName("메시지 작성 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class MessageCreatedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisPaperUpdateAdapter redisPaperUpdateAdapter;

    @MockitoBean
    private NotificationCommandService notificationCommandService;

    private static final double MESSAGE_SCORE = 5.0;

    @Test
    @DisplayName("메시지 작성 이벤트 워크플로우 - 실시간 인기 점수 증가")
    void messageCreatedEventWorkflow_ShouldIncrementScore() {
        // Given
        Long paperOwnerId = testMember.getId();
        String memberName = testMember.getMemberName();
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // When & Then
        publishAndVerify(event, () -> {
            // Redis 점수 증가 검증
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(paperOwnerId), eq(MESSAGE_SCORE));
        });
    }

    @Test
    @DisplayName("여러 다른 롤링페이퍼에 메시지 작성 이벤트 동시 처리")
    void multipleDifferentMessageCreatedEvents_ShouldProcessIndependently() {
        // Given: 3개의 다른 롤링페이퍼에 메시지 작성
        var events = java.util.List.of(
                new RollingPaperEvent(testMember.getId(), testMember.getMemberName()),
                new RollingPaperEvent(otherMember.getId(), otherMember.getMemberName()),
                new RollingPaperEvent(adminMember.getId(), adminMember.getMemberName())
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            // Redis 점수 증가 검증
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(testMember.getId()), eq(MESSAGE_SCORE));
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(otherMember.getId()), eq(MESSAGE_SCORE));
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(adminMember.getId()), eq(MESSAGE_SCORE));
        });
    }

    @Test
    @DisplayName("동일 롤링페이퍼에 여러 메시지 작성 이벤트 처리")
    void multipleMessageEventsForSamePaper_ShouldProcessAll() {
        // Given: 동일 롤링페이퍼에 3번의 메시지 작성
        var events = new java.util.ArrayList<RollingPaperEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new RollingPaperEvent(testMember.getId(), "사용자" + i));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            // Redis 점수 증가 검증
            verify(redisPaperUpdateAdapter, times(3)).incrementRealtimePopularPaperScore(eq(testMember.getId()), eq(MESSAGE_SCORE));
        });
    }

    @Test
    @DisplayName("점수 증가 실패 시에도 시스템 정상 작동")
    void messageCreatedEventWithException_ShouldContinueWorking() {
        // Given
        Long paperOwnerId = testMember.getId();
        String memberName = testMember.getMemberName();
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // 점수 증가 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("Redis 점수 증가 실패"))
                .when(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(paperOwnerId, MESSAGE_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(paperOwnerId), eq(MESSAGE_SCORE));
        });
    }

    @Test
    @DisplayName("대량 메시지 작성 이벤트 처리")
    void bulkMessageCreatedEvents_ShouldProcessAll() {
        // Given: testMember 롤링페이퍼에 10번의 메시지 작성
        var events = new java.util.ArrayList<RollingPaperEvent>();
        for (int i = 1; i <= 10; i++) {
            events.add(new RollingPaperEvent(testMember.getId(), "사용자" + i));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            // Redis 점수 증가 검증
            verify(redisPaperUpdateAdapter, times(10)).incrementRealtimePopularPaperScore(eq(testMember.getId()), eq(MESSAGE_SCORE));
        });
    }

    @Test
    @DisplayName("이벤트 발행 후 즉시 다른 이벤트 발행")
    void consecutiveEvents_ShouldProcessBoth() {
        // Given
        RollingPaperEvent event1 = new RollingPaperEvent(testMember.getId(), testMember.getMemberName());
        RollingPaperEvent event2 = new RollingPaperEvent(otherMember.getId(), otherMember.getMemberName());

        // When: 연속으로 이벤트 발행
        publishEvent(event1);
        publishEvent(event2);

        // Then: 두 이벤트 모두 처리됨
        verifyAsync(() -> {
            // Redis 점수 증가 검증
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(testMember.getId()), eq(MESSAGE_SCORE));
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(otherMember.getId()), eq(MESSAGE_SCORE));
        });
    }
}
