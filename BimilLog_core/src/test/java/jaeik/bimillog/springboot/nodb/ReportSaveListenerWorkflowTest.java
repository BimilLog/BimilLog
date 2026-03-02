package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.listener.ReportSaveListener;
import jaeik.bimillog.domain.admin.service.AdminCommandService;
import jaeik.bimillog.domain.member.event.ReportSubmittedEvent;
import jaeik.bimillog.infrastructure.config.async.AsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>ReportSaveListener 워크플로우 테스트</h2>
 * <p>다양한 신고 유형 및 예외 처리 시나리오를 검증하는 리스너 기반 테스트</p>
 */
@DisplayName("ReportSaveListener 워크플로우 테스트")
@SpringBootTest(classes = {ReportSaveListener.class, RetryConfig.class, AsyncConfig.class})
@Tag("springboot-nodb")
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class ReportSaveListenerWorkflowTest {

    @Autowired
    private ReportSaveListener listener;

    @MockitoBean
    private AdminCommandService adminCommandService;

    @BeforeEach
    void setUp() {
        Mockito.reset(adminCommandService);
    }

    @Test
    @DisplayName("인증된 사용자 댓글 신고 처리")
    void handleReportSubmitted_AuthenticatedUser_CommentReport() {
        // Given
        var event = ReportSubmittedEvent.of(1L, "testuser", ReportType.COMMENT, 123L, "부적절한 댓글입니다");

        // When
        listener.handleReportSubmitted(event);

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandService).createReport(eq(1L), eq(ReportType.COMMENT), eq(123L), eq("부적절한 댓글입니다"));
                    verifyNoMoreInteractions(adminCommandService);
                });
    }

    @Test
    @DisplayName("익명 사용자 게시글 신고 처리")
    void handleReportSubmitted_AnonymousUser_PostReport() {
        // Given
        var event = ReportSubmittedEvent.of(null, "익명", ReportType.POST, 456L, "스팸 게시글입니다");

        // When
        listener.handleReportSubmitted(event);

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandService).createReport(eq(null), eq(ReportType.POST), eq(456L), eq("스팸 게시글입니다"));
                    verifyNoMoreInteractions(adminCommandService);
                });
    }

    @Test
    @DisplayName("건의사항 처리 - targetId 없음")
    void handleReportSubmitted_Improvement_NoTargetId() {
        // Given
        var event = ReportSubmittedEvent.of(2L, "suggester", ReportType.IMPROVEMENT, null, "새로운 기능을 건의합니다");

        // When
        listener.handleReportSubmitted(event);

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandService).createReport(eq(2L), eq(ReportType.IMPROVEMENT), eq(null), eq("새로운 기능을 건의합니다"));
                    verifyNoMoreInteractions(adminCommandService);
                });
    }

    @Test
    @DisplayName("여러 신고 이벤트 독립 처리")
    void handleReportSubmitted_MultipleEvents_ProcessIndependently() {
        // When
        listener.handleReportSubmitted(ReportSubmittedEvent.of(1L, "testuser", ReportType.COMMENT, 100L, "부적절한 댓글입니다"));
        listener.handleReportSubmitted(ReportSubmittedEvent.of(null, "익명", ReportType.POST, 200L, "스팸 게시글입니다"));
        listener.handleReportSubmitted(ReportSubmittedEvent.of(3L, "suggester", ReportType.IMPROVEMENT, null, "새로운 기능을 건의합니다"));

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandService).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("부적절한 댓글입니다"));
                    verify(adminCommandService).createReport(eq(null), eq(ReportType.POST), eq(200L), eq("스팸 게시글입니다"));
                    verify(adminCommandService).createReport(eq(3L), eq(ReportType.IMPROVEMENT), eq(null), eq("새로운 기능을 건의합니다"));
                    verifyNoMoreInteractions(adminCommandService);
                });
    }

    @Test
    @DisplayName("비즈니스 예외 발생 시 재시도 없이 1회만 호출")
    void handleReportSubmitted_BusinessException_CalledOnce() {
        // Given
        var event = ReportSubmittedEvent.of(1L, "user1", ReportType.COMMENT, 100L, "중복 신고");
        doThrow(new IllegalStateException("이미 처리된 신고입니다"))
                .when(adminCommandService).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("중복 신고"));

        // When
        listener.handleReportSubmitted(event);

        // Then: IllegalStateException은 재시도 대상 아님 → 1회만 호출
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(adminCommandService, times(1)).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("중복 신고"))
                );
    }

    @Test
    @DisplayName("예외 발생 시 다른 이벤트 처리에 영향 없음")
    void handleReportSubmitted_ExceptionIsolation() {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(adminCommandService).createReport(eq(999L), any(ReportType.class), anyLong(), anyString());

        // When
        listener.handleReportSubmitted(ReportSubmittedEvent.of(1L, "user1", ReportType.COMMENT, 100L, "정상 신고"));
        listener.handleReportSubmitted(ReportSubmittedEvent.of(999L, "baduser", ReportType.POST, 200L, "실패할 신고"));

        // Then: 각 이벤트가 독립적으로 처리됨
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandService).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("정상 신고"));
                    verify(adminCommandService).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("실패할 신고"));
                });
    }
}
