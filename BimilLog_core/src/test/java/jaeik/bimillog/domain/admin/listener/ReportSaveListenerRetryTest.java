package jaeik.bimillog.domain.admin.listener;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.service.AdminCommandService;
import jaeik.bimillog.domain.member.event.ReportSubmittedEvent;
import jaeik.bimillog.infrastructure.config.AsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>ReportSaveListener 재시도 테스트</h2>
 * <p>DB 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("ReportSaveListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {ReportSaveListener.class, RetryConfig.class, AsyncConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class ReportSaveListenerRetryTest {

    @Autowired
    private ReportSaveListener listener;

    @MockitoBean
    private AdminCommandService adminCommandService;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(adminCommandService);
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("DB 관련 예외 발생 시 재시도")
    void shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        ReportSubmittedEvent event = ReportSubmittedEvent.of(1L, "신고자", ReportType.POST, 100L, "신고 내용");
        willThrow(exception)
                .given(adminCommandService).createReport(anyLong(), any(ReportType.class), anyLong(), anyString());

        // When: 비동기로 실행되며 @Recover 메서드가 있으므로 예외가 외부로 전파되지 않음
        listener.handleReportSubmitted(event);

        // Then: 비동기 완료 대기 후 재시도 횟수만큼 호출되었는지 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(adminCommandService, times(MAX_ATTEMPTS))
                        .createReport(1L, ReportType.POST, 100L, "신고 내용"));
    }

    private static Stream<Arguments> provideRetryableExceptions() {
        return Stream.of(
                Arguments.of("TransientDataAccessException",
                        new TransientDataAccessException("일시적 DB 오류") {}),
                Arguments.of("DataAccessResourceFailureException",
                        new DataAccessResourceFailureException("DB 리소스 획득 실패")),
                Arguments.of("QueryTimeoutException",
                        new QueryTimeoutException("쿼리 타임아웃"))
        );
    }

    @Test
    @DisplayName("2회 실패 후 3회차에 성공하면 총 3회 호출")
    void shouldSucceedAfterTwoFailures() {
        // Given
        ReportSubmittedEvent event = ReportSubmittedEvent.of(1L, "신고자", ReportType.COMMENT, 200L, "댓글 신고");
        willThrow(new DataAccessResourceFailureException("실패"))
                .willThrow(new DataAccessResourceFailureException("실패"))
                .willDoNothing()
                .given(adminCommandService).createReport(anyLong(), any(ReportType.class), anyLong(), anyString());

        // When
        listener.handleReportSubmitted(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(adminCommandService, times(3))
                        .createReport(1L, ReportType.COMMENT, 200L, "댓글 신고"));
    }

    @Test
    @DisplayName("1회 성공 시 재시도 없이 1회만 호출")
    void shouldNotRetryOnSuccess() {
        // Given
        ReportSubmittedEvent event = ReportSubmittedEvent.of(1L, "신고자", ReportType.IMPROVEMENT, 0L, "건의사항");
        doNothing().when(adminCommandService).createReport(anyLong(), any(ReportType.class), anyLong(), anyString());

        // When
        listener.handleReportSubmitted(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(adminCommandService, times(1))
                        .createReport(1L, ReportType.IMPROVEMENT, 0L, "건의사항"));
    }
}
