package jaeik.bimillog.integration.event;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.entity.ReportVO;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import jaeik.bimillog.infrastructure.adapter.admin.in.listener.ReportSaveListener;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>신고 제출 이벤트 워크플로우 통합 테스트</h2>
 * <p>신고 제출부터 저장까지의 전체 이벤트 기반 워크플로우를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리를 Awaitility를 사용하여 검증합니다.</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("신고 제출 이벤트 워크플로우 통합 테스트")
class ReportSubmissionEventWorkflowIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ReportSaveListener reportSaveListener;

    @MockitoBean
    private AdminCommandUseCase adminCommandUseCase;

    @MockitoBean
    private AdminCommandPort adminCommandPort;

    @MockitoBean
    private UserQueryPort userQueryPort;

    @Test
    @DisplayName("인증된 사용자 신고 이벤트 워크플로우 - 성공")
    void reportSubmissionWorkflow_AuthenticatedUser_Success() {
        // Given
        Long reporterId = 1L;
        String reporterName = "testuser";
        ReportVO reportVO = ReportVO.of(ReportType.COMMENT, 123L, "부적절한 댓글입니다");

        User mockReporter = User.builder()
                .id(reporterId)
                .userName(reporterName)
                .build();

        when(userQueryPort.findById(reporterId)).thenReturn(Optional.of(mockReporter));
        when(adminCommandPort.save(any(Report.class))).thenReturn(mock(Report.class));

        // When
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);
        eventPublisher.publishEvent(event);

        // Then - 비동기 이벤트 처리 완료까지 대기 (최대 5초)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
                });
    }

    @Test
    @DisplayName("익명 사용자 신고 이벤트 워크플로우 - 성공")
    void reportSubmissionWorkflow_AnonymousUser_Success() {
        // Given
        Long reporterId = null; // 익명 사용자
        String reporterName = "익명";
        ReportVO reportVO = ReportVO.of(ReportType.POST, 456L, "스팸 게시글입니다");

        when(adminCommandPort.save(any(Report.class))).thenReturn(mock(Report.class));

        // When
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);
        eventPublisher.publishEvent(event);

        // Then - 비동기 이벤트 처리 완료까지 대기 (최대 5초)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
                });
    }

    @Test
    @DisplayName("건의사항 이벤트 워크플로우 - 성공")
    void reportSubmissionWorkflow_Suggestion_Success() {
        // Given
        Long reporterId = 2L;
        String reporterName = "suggester";
        ReportVO reportVO = ReportVO.of(ReportType.IMPROVEMENT, null, "새로운 기능을 건의합니다");

        User mockReporter = User.builder()
                .id(reporterId)
                .userName(reporterName)
                .build();

        when(userQueryPort.findById(reporterId)).thenReturn(Optional.of(mockReporter));
        when(adminCommandPort.save(any(Report.class))).thenReturn(mock(Report.class));

        // When
        ReportSubmittedEvent event = ReportSubmittedEvent.of(reporterId, reporterName, reportVO);
        eventPublisher.publishEvent(event);

        // Then - 비동기 이벤트 처리 완료까지 대기 (최대 5초)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase, times(1)).createReport(eq(reporterId), eq(reportVO));
                });
    }

    @Test
    @DisplayName("여러 신고 이벤트 동시 처리 워크플로우 - 성공")
    void reportSubmissionWorkflow_MultipleEvents_Success() {
        // Given
        ReportSubmittedEvent event1 = ReportSubmittedEvent.of(
                1L, "user1", ReportVO.of(ReportType.COMMENT, 100L, "신고 내용 1"));
        ReportSubmittedEvent event2 = ReportSubmittedEvent.of(
                null, "익명", ReportVO.of(ReportType.POST, 200L, "신고 내용 2"));
        ReportSubmittedEvent event3 = ReportSubmittedEvent.of(
                3L, "user3", ReportVO.of(ReportType.IMPROVEMENT, null, "건의 내용"));

        User mockUser1 = User.builder().id(1L).userName("user1").build();
        User mockUser3 = User.builder().id(3L).userName("user3").build();

        when(userQueryPort.findById(1L)).thenReturn(Optional.of(mockUser1));
        when(userQueryPort.findById(3L)).thenReturn(Optional.of(mockUser3));
        when(adminCommandPort.save(any(Report.class))).thenReturn(mock(Report.class));

        // When - 여러 이벤트 동시 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 이벤트가 비동기로 처리될 때까지 대기 (최대 10초)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase, times(1)).createReport(eq(1L), eq(event1.reportVO()));
                    verify(adminCommandUseCase, times(1)).createReport(eq(null), eq(event2.reportVO()));
                    verify(adminCommandUseCase, times(1)).createReport(eq(3L), eq(event3.reportVO()));
                    verify(adminCommandUseCase, times(3)).createReport(any(), any());
                });
    }

    @Test
    @DisplayName("이벤트 처리 중 예외 발생 시 다른 이벤트 처리에 영향 없음")
    void reportSubmissionWorkflow_ExceptionInOneEvent_OthersStillProcessed() {
        // Given
        ReportSubmittedEvent successEvent = ReportSubmittedEvent.of(
                1L, "user1", ReportVO.of(ReportType.COMMENT, 100L, "정상 신고"));
        ReportSubmittedEvent failureEvent = ReportSubmittedEvent.of(
                999L, "baduser", ReportVO.of(ReportType.POST, 200L, "실패할 신고"));

        User mockUser1 = User.builder().id(1L).userName("user1").build();

        when(userQueryPort.findById(1L)).thenReturn(Optional.of(mockUser1));
        when(adminCommandPort.save(any(Report.class))).thenReturn(mock(Report.class));

        // failureEvent는 예외 발생하도록 설정
        doThrow(new RuntimeException("Database connection failed"))
                .when(adminCommandUseCase).createReport(eq(999L), any());
        doNothing().when(adminCommandUseCase).createReport(eq(1L), any());

        // When
        eventPublisher.publishEvent(successEvent);
        eventPublisher.publishEvent(failureEvent);

        // Then - 성공 이벤트는 처리되고, 실패 이벤트도 시도는 됨
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase, times(1)).createReport(eq(1L), eq(successEvent.reportVO()));
                    verify(adminCommandUseCase, times(1)).createReport(eq(999L), eq(failureEvent.reportVO()));
                });
    }

    @Test
    @DisplayName("이벤트 리스너가 정상적으로 등록되어 있는지 검증")
    void reportEventListener_IsRegistered() {
        // Given & When & Then
        // ReportEventListener가 Spring 컨텍스트에 정상적으로 등록되었는지 확인
        assert reportSaveListener != null;
        assert eventPublisher != null;
    }

    @Test
    @DisplayName("빠른 연속 이벤트 처리 성능 테스트")
    void reportSubmissionWorkflow_RapidEvents_Performance() {
        // Given
        int eventCount = 50;
        when(adminCommandPort.save(any(Report.class))).thenReturn(mock(Report.class));

        // When - 빠른 연속으로 여러 이벤트 발행
        for (int i = 0; i < eventCount; i++) {
            ReportSubmittedEvent event = ReportSubmittedEvent.of(
                    null, "익명", ReportVO.of(ReportType.COMMENT, (long) i, "신고 내용 " + i));
            eventPublisher.publishEvent(event);
        }

        // Then - 모든 이벤트가 처리될 때까지 대기 (최대 30초)
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    verify(adminCommandUseCase, times(eventCount)).createReport(eq(null), any(ReportVO.class));
                });
    }
}