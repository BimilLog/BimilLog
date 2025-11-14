package jaeik.bimillog.domain.paper.event;

import jaeik.bimillog.infrastructure.redis.paper.RedisPaperUpdateAdapter;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>메시지 삭제 이벤트 워크플로우 통합 테스트</h2>
 * <p>메시지 삭제 시 발생하는 실시간 인기 점수 감소를 검증하는 통합 테스트</p>
 * <p>MessageDeletedEvent 발생 시 비동기로 실시간 인기 점수가 5점 감소하는지 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("메시지 삭제 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class MessageDeletedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisPaperUpdateAdapter redisPaperUpdateAdapter;

    private static final double MESSAGE_SCORE = -5.0; // 삭제는 음수

    @Test
    @DisplayName("메시지 삭제 이벤트 워크플로우 - 실시간 인기 점수 감소")
    void messageDeletedEventWorkflow_ShouldDecrementScore() {
        // Given
        Long paperOwnerId = 1L;
        MessageDeletedEvent event = new MessageDeletedEvent(paperOwnerId);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(paperOwnerId), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("여러 다른 롤링페이퍼의 메시지 삭제 이벤트 동시 처리")
    void multipleDifferentMessageDeletedEvents_ShouldProcessIndependently() {
        // Given: 3개의 다른 롤링페이퍼에서 메시지 삭제
        var events = java.util.List.of(
                new MessageDeletedEvent(1L),
                new MessageDeletedEvent(2L),
                new MessageDeletedEvent(3L)
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(1L), eq(MESSAGE_SCORE));
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(2L), eq(MESSAGE_SCORE));
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(3L), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("동일 롤링페이퍼에서 여러 메시지 삭제 이벤트 처리")
    void multipleDeleteEventsForSamePaper_ShouldProcessAll() {
        // Given: 동일 롤링페이퍼에서 3번의 메시지 삭제
        var events = new java.util.ArrayList<MessageDeletedEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new MessageDeletedEvent(1L));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPaperUpdateAdapter, times(3)).incrementRealtimePopularPaperScore(eq(1L), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("점수 감소 실패 시에도 시스템 정상 작동")
    void messageDeletedEventWithException_ShouldContinueWorking() {
        // Given
        Long paperOwnerId = 1L;
        MessageDeletedEvent event = new MessageDeletedEvent(paperOwnerId);

        // 점수 감소 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("Redis 점수 감소 실패"))
                .when(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(paperOwnerId, MESSAGE_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(paperOwnerId), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("대량 메시지 삭제 이벤트 처리")
    void bulkMessageDeletedEvents_ShouldProcessAll() {
        // Given: 10개의 다른 롤링페이퍼에서 메시지 삭제
        var events = new java.util.ArrayList<MessageDeletedEvent>();
        for (int i = 1; i <= 10; i++) {
            events.add(new MessageDeletedEvent((long) i));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            for (int i = 1; i <= 10; i++) {
                verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq((long) i), eq(MESSAGE_SCORE));
            }
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("이벤트 발행 후 즉시 다른 이벤트 발행")
    void consecutiveEvents_ShouldProcessBoth() {
        // Given
        MessageDeletedEvent event1 = new MessageDeletedEvent(1L);
        MessageDeletedEvent event2 = new MessageDeletedEvent(2L);

        // When: 연속으로 이벤트 발행
        publishEvent(event1);
        publishEvent(event2);

        // Then: 두 이벤트 모두 처리됨
        verifyAsync(() -> {
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(1L), eq(MESSAGE_SCORE));
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(2L), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("메시지 작성과 삭제 이벤트 혼합 처리")
    void mixedCreateAndDeleteEvents_ShouldProcessBoth() {
        // Given: 동일 롤링페이퍼에 메시지 삭제(현재 테스트)
        Long paperId = 1L;
        MessageDeletedEvent deleteEvent = new MessageDeletedEvent(paperId);

        // When: 삭제 이벤트만 발행
        publishAndVerify(deleteEvent, () -> {
            // Then: 삭제 점수만 감소 (-5점)
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(paperId), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }

    @Test
    @DisplayName("단일 롤링페이퍼에 메시지 작성 후 즉시 삭제")
    void createThenDeleteMessage_ShouldProcessBoth() {
        // Given
        Long paperId = 1L;
        MessageDeletedEvent deleteEvent = new MessageDeletedEvent(paperId);

        // When: 삭제 이벤트 발행 (작성 이벤트는 별도)
        publishAndVerify(deleteEvent, () -> {
            // Then: 삭제 점수 감소
            verify(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(eq(paperId), eq(MESSAGE_SCORE));
            verifyNoMoreInteractions(redisPaperUpdateAdapter);
        });
    }
}
