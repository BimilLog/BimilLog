package jaeik.bimillog.integration.event.user;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

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
@SpringBootTest
@Import(TestContainersConfiguration.class)
@Testcontainers
@Transactional
@DisplayName("신고 제출 이벤트 워크플로우 통합 테스트")
class ReportSubmittedEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private AdminCommandUseCase adminCommandUseCase;

    @Test
    @DisplayName("인증된 사용자 신고 이벤트 워크플로우 - 댓글 신고")
    void reportSubmissionWorkflow_AuthenticatedUser_CommentReport() {
        // Given
        Long reporterId = 1L;
        String reporterName = "testuser";
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";

        // When
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportType, targetId, content);
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(reporterId), eq(reportType), eq(targetId), eq(content));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("익명 사용자 신고 이벤트 워크플로우 - 게시글 신고")
    void reportSubmissionWorkflow_AnonymousUser_PostReport() {
        // Given
        Long reporterId = null; // 익명 사용자
        String reporterName = "익명";
        ReportType reportType = ReportType.POST;
        Long targetId = 456L;
        String content = "스팸 게시글입니다";

        // When
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportType, targetId, content);
        eventPublisher.publishEvent(event);

        // Then - 익명 사용자도 정상적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(null), eq(reportType), eq(targetId), eq(content));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("건의사항 이벤트 워크플로우 - targetId 없음")
    void reportSubmissionWorkflow_Improvement_NoTargetId() {
        // Given
        Long reporterId = 2L;
        String reporterName = "suggester";
        ReportType reportType = ReportType.IMPROVEMENT;
        Long targetId = null; // 건의사항은 특정 타겟이 없을 수 있음
        String content = "새로운 기능을 건의합니다";

        // When
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportType, targetId, content);
        eventPublisher.publishEvent(event);

        // Then - targetId가 null이어도 정상 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(reporterId), eq(reportType), eq(null), eq(content));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("여러 신고 이벤트 동시 처리")
    void reportSubmissionWorkflow_MultipleEvents_ProcessIndependently() {
        // Given
        ReportSubmittedEvent event1 = ReportSubmittedEvent.of(
                1L, "user1", ReportType.COMMENT, 100L, "신고 내용 1");
        ReportSubmittedEvent event2 = ReportSubmittedEvent.of(
                null, "익명", ReportType.POST, 200L, "신고 내용 2");
        ReportSubmittedEvent event3 = ReportSubmittedEvent.of(
                3L, "user3", ReportType.IMPROVEMENT, null, "건의 내용");

        // When - 여러 이벤트 동시 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("신고 내용 1"));
                    verify(adminCommandUseCase).createReport(eq(null), eq(ReportType.POST), eq(200L), eq("신고 내용 2"));
                    verify(adminCommandUseCase).createReport(eq(3L), eq(ReportType.IMPROVEMENT), eq(null), eq("건의 내용"));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("비즈니스 예외 발생 시 처리")
    void reportSubmissionWorkflow_BusinessExceptionHandling() {
        // Given
        ReportSubmittedEvent event = ReportSubmittedEvent.of(
                1L, "user1", ReportType.COMMENT, 100L, "중복 신고");
        
        // 비즈니스 예외 발생 시뮬레이션 (예: 중복 신고)  
        doThrow(new IllegalStateException("이미 처리된 신고입니다"))
                .when(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("중복 신고"));

        // When
        eventPublisher.publishEvent(event);

        // Then - 비즈니스 예외가 발생해도 리스너는 호출되고 예외를 삼켜야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("중복 신고"));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("시스템 예외 발생 시 처리")
    void reportSubmissionWorkflow_SystemExceptionHandling() {
        // Given
        ReportSubmittedEvent event = ReportSubmittedEvent.of(
                999L, "baduser", ReportType.POST, 200L, "시스템 오류 발생");
        
        // 시스템 예외 발생 시뮬레이션 (예: DB 연결 실패)
        doThrow(new RuntimeException("Database connection failed"))
                .when(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("시스템 오류 발생"));

        // When
        eventPublisher.publishEvent(event);

        // Then - 시스템 예외가 발생해도 리스너는 호출되고 예외를 삼켜야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("시스템 오류 발생"));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("이벤트 처리 중 예외 발생 시 다른 이벤트에 영향 없음")
    void reportSubmissionWorkflow_ExceptionIsolation() {
        // Given
        ReportSubmittedEvent successEvent = ReportSubmittedEvent.of(
                1L, "user1", ReportType.COMMENT, 100L, "정상 신고");
        ReportSubmittedEvent failureEvent = ReportSubmittedEvent.of(
                999L, "baduser", ReportType.POST, 200L, "실패할 신고");

        // failureEvent는 예외 발생하도록 설정
        doThrow(new RuntimeException("Database connection failed"))
                .when(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("실패할 신고"));

        // When
        eventPublisher.publishEvent(successEvent);
        eventPublisher.publishEvent(failureEvent);

        // Then - 성공 이벤트와 실패 이벤트 모두 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("정상 신고"));
                    verify(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(200L), eq("실패할 신고"));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("모든 신고 유형 처리 검증")
    void reportSubmissionWorkflow_AllReportTypes() {
        // Given - 모든 신고 유형
        ReportSubmittedEvent commentReport = ReportSubmittedEvent.of(
                1L, "user1", ReportType.COMMENT, 100L, "댓글 신고");
        ReportSubmittedEvent postReport = ReportSubmittedEvent.of(
                2L, "user2", ReportType.POST, 200L, "게시글 신고");
        ReportSubmittedEvent improvement = ReportSubmittedEvent.of(
                3L, "user3", ReportType.IMPROVEMENT, null, "기능 개선 건의");

        // When
        eventPublisher.publishEvent(commentReport);
        eventPublisher.publishEvent(postReport);
        eventPublisher.publishEvent(improvement);

        // Then - 모든 신고 유형이 정상 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq("댓글 신고"));
                    verify(adminCommandUseCase).createReport(eq(2L), eq(ReportType.POST), eq(200L), eq("게시글 신고"));
                    verify(adminCommandUseCase).createReport(eq(3L), eq(ReportType.IMPROVEMENT), eq(null), eq("기능 개선 건의"));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("비동기 이벤트 리스너 정상 작동 검증")
    void reportSubmissionEventAsync_ShouldTriggerListenerCorrectly() {
        // Given
        ReportSubmittedEvent event = ReportSubmittedEvent.of(
                999L, "asynctest", ReportType.POST, 9999L, "비동기 테스트 신고");

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리가 정상 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(999L), eq(ReportType.POST), eq(9999L), eq("비동기 테스트 신고"));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }

    @Test
    @DisplayName("특수 문자가 포함된 신고 내용 처리")
    void reportSubmissionWorkflow_SpecialCharacters() {
        // Given - 특수문자가 포함된 신고 내용
        String specialContent = "신고합니다! <script>alert('XSS')</script> & \"악성코드\" 포함됨";
        ReportSubmittedEvent event = ReportSubmittedEvent.of(
                1L, "reporter", ReportType.COMMENT, 100L, specialContent);

        // When
        eventPublisher.publishEvent(event);

        // Then - 특수문자가 포함된 내용도 정상 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase).createReport(eq(1L), eq(ReportType.COMMENT), eq(100L), eq(specialContent));
                    verifyNoMoreInteractions(adminCommandUseCase);
                });
    }
}