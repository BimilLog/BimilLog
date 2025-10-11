package jaeik.bimillog.event.member;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.member.event.ReportSubmittedEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>신고 제출 이벤트 워크플로우 통합 테스트</h2>
 * <p>신고 제출부터 저장까지의 전체 이벤트 기반 워크플로우를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("신고 제출 이벤트 워크플로우 통합 테스트")
@Tag("integration")
class ReportSubmittedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private AdminCommandUseCase adminCommandUseCase;

    @Test
    @DisplayName("인증된 사용자 신고 이벤트 워크플로우 - 댓글 신고")
    void reportSubmissionWorkflow_AuthenticatedUser_CommentReport() {
        // Given
        var event = ReportSubmittedEvent.of(1L, "testuser", ReportType.COMMENT, 123L, "부적절한 댓글입니다");

        // When & Then
        publishAndVerify(event, () -> {
            verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(123L), eq("부적절한 댓글입니다"));
            verifyNoMoreInteractions(adminCommandUseCase);
        });
    }

    @Test
    @DisplayName("익명 사용자 신고 이벤트 워크플로우 - 게시글 신고")
    void reportSubmissionWorkflow_AnonymousUser_PostReport() {
        // Given
        var event = ReportSubmittedEvent.of(null, "익명", ReportType.POST, 456L, "스팸 게시글입니다");

        // When & Then
        publishAndVerify(event, () -> {
            verify(adminCommandUseCase).createReport(eq(null), eq(ReportType.POST), eq(456L), eq("스팸 게시글입니다"));
            verifyNoMoreInteractions(adminCommandUseCase);
        });
    }

    @Test
    @DisplayName("건의사항 이벤트 워크플로우 - targetId 없음")
    void reportSubmissionWorkflow_Improvement_NoTargetId() {
        // Given
        var event = ReportSubmittedEvent.of(2L, "suggester", ReportType.IMPROVEMENT, null, "새로운 기능을 건의합니다");

        // When & Then
        publishAndVerify(event, () -> {
            verify(adminCommandUseCase).createReport(eq(2L), eq(ReportType.IMPROVEMENT), eq(null), eq("새로운 기능을 건의합니다"));
            verifyNoMoreInteractions(adminCommandUseCase);
        });
    }

    @Test
    @DisplayName("여러 신고 이벤트 동시 처리")
    void reportSubmissionWorkflow_MultipleEvents_ProcessIndependently() {
        // Given
        var events = java.util.List.of(
                ReportSubmittedEvent.of(1L, "testuser", ReportType.COMMENT, 100L, "부적절한 댓글입니다"),
                ReportSubmittedEvent.of(null, "익명", ReportType.POST, 200L, "스팸 게시글입니다"),
                ReportSubmittedEvent.of(3L, "suggester", ReportType.IMPROVEMENT, null, "새로운 기능을 건의합니다")
        );

        // When & Then
        publishEventsAndVerify(events.toArray(), () -> {
            verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("부적절한 댓글입니다"));
            verify(adminCommandUseCase).createReport(eq(null), eq(ReportType.POST), eq(200L), eq("스팸 게시글입니다"));
            verify(adminCommandUseCase).createReport(eq(3L), eq(ReportType.IMPROVEMENT), eq(null), eq("새로운 기능을 건의합니다"));
            verifyNoMoreInteractions(adminCommandUseCase);
        });
    }

    @Test
    @DisplayName("비즈니스 예외 발생 시 처리")
    void reportSubmissionWorkflow_BusinessExceptionHandling() {
        // Given
        var event = ReportSubmittedEvent.of(1L, "user1", ReportType.COMMENT, 100L, "중복 신고");

        // 비즈니스 예외 발생 시뮬레이션 (예: 중복 신고)
        doThrow(new IllegalStateException("이미 처리된 신고입니다"))
                .when(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("중복 신고"));

        // When & Then
        publishAndExpectException(event, () -> {
            verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("중복 신고"));
            verifyNoMoreInteractions(adminCommandUseCase);
        });
    }

    @Test
    @DisplayName("이벤트 처리 중 예외 발생 시 다른 이벤트에 영향 없음")
    void reportSubmissionWorkflow_ExceptionIsolation() {
        // Given
        var successEvent = ReportSubmittedEvent.of(1L, "user1", ReportType.COMMENT, 100L, "정상 신고");
        var failureEvent = ReportSubmittedEvent.of(999L, "baduser", ReportType.POST, 200L, "실패할 신고");

        // failureEvent는 예외 발생하도록 설정
        doThrow(new RuntimeException("Database connection failed"))
                .when(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("실패할 신고"));

        // When & Then
        publishEvents(successEvent, failureEvent);
        verifyAsync(() -> {
            verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("정상 신고"));
            verify(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("실패할 신고"));
            verifyNoMoreInteractions(adminCommandUseCase);
        });
    }
}