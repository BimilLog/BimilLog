package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.out.AdminQueryPort;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.admin.entity.ReportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
/**
 * <h2>AdminQueryService 단위 테스트</h2>
 * <p>관리자 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminQueryService 단위 테스트")
class AdminQueryServiceTest {

    @Mock
    private AdminQueryPort adminQueryPort;

    @InjectMocks
    private AdminQueryService adminQueryService;

    private List<ReportSummary> testReports;

    @BeforeEach
    void setUp() {
        testReports = List.of(
                ReportSummary.builder()
                        .id(1L)
                        .reporterId(100L)
                        .reporterName("reporter1")
                        .reportType(ReportType.POST)
                        .targetId(200L)
                        .content("부적절한 게시글")
                        .createdAt(Instant.now())
                        .build(),
                ReportSummary.builder()
                        .id(2L)
                        .reporterId(101L)
                        .reporterName("reporter2")
                        .reportType(ReportType.COMMENT)
                        .targetId(201L)
                        .content("욕설 댓글")
                        .createdAt(Instant.now().minusSeconds(3600))
                        .build(),
                ReportSummary.builder()
                        .id(3L)
                        .reporterId(102L)
                        .reporterName("reporter3")
                        .reportType(ReportType.PAPER)
                        .targetId(202L)
                        .content("스팸 메시지")
                        .createdAt(Instant.now().minusSeconds(7200))
                        .build()
        );
    }

    @Test
    @DisplayName("신고 목록 조회 시 올바른 페이지네이션 및 정렬 적용")
    void shouldGetReportList_WithCorrectPaginationAndSorting() {
        // Given
        int page = 0;
        int size = 10;
        ReportType reportType = ReportType.POST;
        Page<ReportSummary> expectedPage = new PageImpl<>(testReports);

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<ReportSummary> result = adminQueryService.getReportList(page, size, reportType);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(3);

        // Pageable 검증
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("신고 유형 필터 없이 전체 신고 목록 조회")
    void shouldGetReportList_WithoutReportTypeFilter() {
        // Given
        int page = 1;
        int size = 5;
        ReportType reportType = null;
        Page<ReportSummary> expectedPage = new PageImpl<>(testReports.subList(0, 2));

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<ReportSummary> result = adminQueryService.getReportList(page, size, reportType);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), any(Pageable.class));

        // Pageable 검증
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(1);
        assertThat(capturedPageable.getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("특정 신고 유형으로 필터링된 신고 목록 조회")
    void shouldGetReportList_WithSpecificReportTypeFilter() {
        // Given
        int page = 0;
        int size = 10;
        ReportType reportType = ReportType.COMMENT;
        List<ReportSummary> commentReports = testReports.stream()
                .filter(report -> report.reportType() == ReportType.COMMENT)
                .toList();
        Page<ReportSummary> expectedPage = new PageImpl<>(commentReports);

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<ReportSummary> result = adminQueryService.getReportList(page, size, reportType);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().reportType()).isEqualTo(ReportType.COMMENT);
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), any(Pageable.class));
    }

    @Test
    @DisplayName("빈 페이지 결과 처리")
    void shouldHandleEmptyPageResult() {
        // Given
        int page = 99;
        int size = 10;
        ReportType reportType = ReportType.POST;
        Page<ReportSummary> emptyPage = new PageImpl<>(List.of());

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(emptyPage);

        // When
        Page<ReportSummary> result = adminQueryService.getReportList(page, size, reportType);

        // Then
        assertThat(result).isEqualTo(emptyPage);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), any(Pageable.class));
    }

    @Test
    @DisplayName("페이지 크기 20으로 신고 목록 조회")
    void shouldGetReportList_WithPageSize20() {
        // Given
        int size = 20;
        Page<ReportSummary> expectedPage = new PageImpl<>(testReports);
        given(adminQueryPort.findReportsWithPaging(any(), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<ReportSummary> result = adminQueryService.getReportList(0, size, null);

        // Then
        assertThat(result).isNotNull();
        
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminQueryPort).findReportsWithPaging(any(), pageableCaptor.capture());
        
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isEqualTo(size);
    }

    @Test
    @DisplayName("올바른 정렬 조건 확인 - createdAt DESC")
    void shouldApplyCorrectSorting_CreatedAtDescending() {
        // Given
        Page<ReportSummary> expectedPage = new PageImpl<>(testReports);
        given(adminQueryPort.findReportsWithPaging(any(), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        adminQueryService.getReportList(0, 10, null);

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminQueryPort).findReportsWithPaging(any(), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        Sort sort = capturedPageable.getSort();
        
        assertThat(sort.isSorted()).isTrue();
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        assertThat(createdAtOrder).isNotNull();
        assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("최대 페이지 크기 제한 테스트")
    void shouldHandleLargePageSize() {
        // Given
        int largePageSize = 1000;
        Page<ReportSummary> expectedPage = new PageImpl<>(testReports);
        given(adminQueryPort.findReportsWithPaging(any(), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<ReportSummary> result = adminQueryService.getReportList(0, largePageSize, null);

        // Then
        assertThat(result).isNotNull();
        
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminQueryPort).findReportsWithPaging(any(), pageableCaptor.capture());
        
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isEqualTo(largePageSize);
    }

    @Test
    @DisplayName("음수 페이지 번호 처리")
    void shouldThrowException_WhenNegativePageNumber() {
        // Given
        int negativePage = -1;

        // When & Then
        assertThatThrownBy(() -> adminQueryService.getReportList(negativePage, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page index must not be less than zero");
    }

    @Test
    @DisplayName("모든 신고 유형에 대한 개별 조회 테스트")
    void shouldGetReportListForAllReportTypes() {
        // Given
        ReportType[] allTypes = {ReportType.POST, ReportType.COMMENT, ReportType.PAPER};
        
        for (ReportType type : allTypes) {
            List<ReportSummary> filteredReports = testReports.stream()
                    .filter(report -> report.reportType() == type)
                    .toList();
            Page<ReportSummary> expectedPage = new PageImpl<>(filteredReports);
            
            given(adminQueryPort.findReportsWithPaging(eq(type), any(Pageable.class)))
                    .willReturn(expectedPage);
        }

        // When & Then
        for (ReportType type : allTypes) {
            Page<ReportSummary> result = adminQueryService.getReportList(0, 10, type);
            assertThat(result).isNotNull();
            verify(adminQueryPort).findReportsWithPaging(eq(type), any(Pageable.class));
        }
    }
}