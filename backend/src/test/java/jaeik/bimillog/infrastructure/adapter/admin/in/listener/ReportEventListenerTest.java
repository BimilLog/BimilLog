package jaeik.bimillog.infrastructure.adapter.admin.in.listener;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.entity.ReportVO;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>ReportEventListener 단위 테스트</h2>
 * <p>신고 이벤트 리스너의 이벤트 처리 로직에 대한 단위 테스트</p>
 * <p>Given-When-Then 패턴을 사용하여 테스트를 구조화합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class ReportEventListenerTest {

    @Mock
    private AdminCommandUseCase adminCommandUseCase;

    @InjectMocks
    private ReportEventListener reportEventListener;

    @Test
    @DisplayName("인증된 사용자 신고 이벤트 처리 - 성공")
    void handleReportSubmitted_AuthenticatedUser_Success() {
        // Given
        Long reporterId = 1L;
        String reporterName = "testuser";
        ReportVO reportVO = ReportVO.of(ReportType.COMMENT, 123L, "부적절한 댓글입니다");
        
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);

        // When
        reportEventListener.handleReportSubmitted(event);

        // Then
        verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
    }

    @Test
    @DisplayName("익명 사용자 신고 이벤트 처리 - 성공")
    void handleReportSubmitted_AnonymousUser_Success() {
        // Given
        Long reporterId = null; // 익명 사용자
        String reporterName = "익명";
        ReportVO reportVO = ReportVO.of(ReportType.POST, 456L, "스팸 게시글입니다");
        
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);

        // When
        reportEventListener.handleReportSubmitted(event);

        // Then
        verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
    }

    @Test
    @DisplayName("건의사항 이벤트 처리 - 성공")
    void handleReportSubmitted_Suggestion_Success() {
        // Given
        Long reporterId = 2L;
        String reporterName = "suggester";
        ReportVO reportVO = ReportVO.of(ReportType.SUGGESTION, null, "새로운 기능을 건의합니다");
        
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);

        // When
        reportEventListener.handleReportSubmitted(event);

        // Then
        verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
    }

    @Test
    @DisplayName("신고 이벤트 처리 중 예외 발생 - 로그 기록 후 계속 진행")
    void handleReportSubmitted_Exception_ContinueExecution() {
        // Given
        Long reporterId = 1L;
        String reporterName = "testuser";
        ReportVO reportVO = ReportVO.of(ReportType.COMMENT, 123L, "부적절한 댓글입니다");
        
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);
        
        // AdminCommandUseCase에서 예외 발생 시뮬레이션
        doThrow(new RuntimeException("Database connection failed"))
                .when(adminCommandUseCase).createReport(any(), any());

        // When
        reportEventListener.handleReportSubmitted(event);

        // Then
        verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
        // 예외가 발생해도 메서드가 정상 종료되어야 함 (로깅 후 계속 진행)
    }

    @Test
    @DisplayName("여러 이벤트 연속 처리 - 성공")
    void handleMultipleReportSubmittedEvents_Success() {
        // Given
        ReportSubmittedEvent event1 = ReportSubmittedEvent.of(
                1L, "user1", ReportVO.of(ReportType.COMMENT, 100L, "신고 내용 1"));
        ReportSubmittedEvent event2 = ReportSubmittedEvent.of(
                null, "익명", ReportVO.of(ReportType.POST, 200L, "신고 내용 2"));
        ReportSubmittedEvent event3 = ReportSubmittedEvent.of(
                3L, "user3", ReportVO.of(ReportType.SUGGESTION, null, "건의 내용"));

        // When
        reportEventListener.handleReportSubmitted(event1);
        reportEventListener.handleReportSubmitted(event2);
        reportEventListener.handleReportSubmitted(event3);

        // Then
        verify(adminCommandUseCase, times(1)).createReport(eq(1L), eq(event1.reportVO()));
        verify(adminCommandUseCase, times(1)).createReport(eq(null), eq(event2.reportVO()));
        verify(adminCommandUseCase, times(1)).createReport(eq(3L), eq(event3.reportVO()));
        verify(adminCommandUseCase, times(3)).createReport(any(), any());
    }
}