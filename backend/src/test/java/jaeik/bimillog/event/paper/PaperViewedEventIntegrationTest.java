package jaeik.bimillog.event.paper;

import jaeik.bimillog.domain.paper.application.port.out.RedisPaperUpdatePort;
import jaeik.bimillog.domain.paper.event.PaperViewedEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>롤링페이퍼 조회 이벤트 워크플로우 통합 테스트</h2>
 * <p>롤링페이퍼 조회 시 발생하는 실시간 인기 점수 증가를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("롤링페이퍼 조회 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class PaperViewedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisPaperUpdatePort redisPaperUpdatePort;

    private static final double VIEW_SCORE = 2.0;

    @Test
    @DisplayName("롤링페이퍼 조회 이벤트 워크플로우 - 실시간 인기 점수 증가")
    void paperViewedEventWorkflow_ShouldIncrementScore() {
        // Given
        Long memberId = 1L;
        PaperViewedEvent event = new PaperViewedEvent(memberId);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(memberId), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPaperUpdatePort);
        });
    }

    @Test
    @DisplayName("여러 다른 롤링페이퍼 조회 이벤트 동시 처리")
    void multipleDifferentPaperViewedEvents_ShouldProcessIndependently() {
        // Given
        var events = java.util.List.of(
                new PaperViewedEvent(1L),
                new PaperViewedEvent(2L),
                new PaperViewedEvent(3L)
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(1L), eq(VIEW_SCORE));
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(2L), eq(VIEW_SCORE));
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(3L), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPaperUpdatePort);
        });
    }

    @Test
    @DisplayName("동일 롤링페이퍼의 여러 조회 이벤트 처리")
    void multipleViewEventsForSamePaper_ShouldProcessAll() {
        // Given: 동일 롤링페이퍼에 대한 3번의 조회
        var events = new java.util.ArrayList<PaperViewedEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new PaperViewedEvent(1L));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPaperUpdatePort, times(3)).incrementRealtimePopularPaperScore(eq(1L), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPaperUpdatePort);
        });
    }

    @Test
    @DisplayName("점수 증가 실패 시에도 시스템 정상 작동")
    void paperViewedEventWithException_ShouldContinueWorking() {
        // Given
        Long memberId = 1L;
        PaperViewedEvent event = new PaperViewedEvent(memberId);

        // 점수 증가 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("Redis 점수 증가 실패"))
                .when(redisPaperUpdatePort).incrementRealtimePopularPaperScore(memberId, VIEW_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(memberId), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPaperUpdatePort);
        });
    }

    @Test
    @DisplayName("대량 조회 이벤트 처리")
    void bulkViewEvents_ShouldProcessAll() {
        // Given: 10개의 다른 롤링페이퍼 조회
        var events = new java.util.ArrayList<PaperViewedEvent>();
        for (int i = 1; i <= 10; i++) {
            events.add(new PaperViewedEvent((long) i));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            for (int i = 1; i <= 10; i++) {
                verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq((long) i), eq(VIEW_SCORE));
            }
            verifyNoMoreInteractions(redisPaperUpdatePort);
        });
    }

    @Test
    @DisplayName("이벤트 발행 후 즉시 다른 이벤트 발행")
    void consecutiveEvents_ShouldProcessBoth() {
        // Given
        PaperViewedEvent event1 = new PaperViewedEvent(1L);
        PaperViewedEvent event2 = new PaperViewedEvent(2L);

        // When: 연속으로 이벤트 발행
        publishEvent(event1);
        publishEvent(event2);

        // Then: 두 이벤트 모두 처리됨
        verifyAsync(() -> {
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(1L), eq(VIEW_SCORE));
            verify(redisPaperUpdatePort).incrementRealtimePopularPaperScore(eq(2L), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPaperUpdatePort);
        });
    }
}
