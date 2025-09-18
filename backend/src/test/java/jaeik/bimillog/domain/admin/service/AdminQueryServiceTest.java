package jaeik.bimillog.domain.admin.service;

import jaeik.bimillog.domain.admin.application.port.out.AdminQueryPort;
import jaeik.bimillog.domain.admin.application.service.AdminQueryService;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.testutil.TestUserFactory;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private List<Report> testReports;

    @BeforeEach
    void setUp() {
        User reporter1 = TestUserFactory.builder()
                .withId(100L)
                .withUserName("reporter1")
                .withProvider(SocialProvider.KAKAO)
                .withSocialId("kakao100")
                .build();

        User reporter2 = TestUserFactory.builder()
                .withId(101L)
                .withUserName("reporter2")
                .withProvider(SocialProvider.KAKAO)
                .withSocialId("kakao101")
                .build();

        User reporter3 = TestUserFactory.builder()
                .withId(102L)
                .withUserName("reporter3")
                .withProvider(SocialProvider.KAKAO)
                .withSocialId("kakao102")
                .build();
        
        testReports = List.of(
                Report.builder()
                        .id(1L)
                        .reporter(reporter1)
                        .reportType(ReportType.POST)
                        .targetId(200L)
                        .content("부적절한 게시글")
                        .build(),
                Report.builder()
                        .id(2L)
                        .reporter(reporter2)
                        .reportType(ReportType.COMMENT)
                        .targetId(201L)
                        .content("욕설 댓글")
                        .build(),
                Report.builder()
                        .id(3L)
                        .reporter(reporter3)
                        .reportType(ReportType.IMPROVEMENT)
                        .targetId(null)
                        .content("기능 개선 건의")
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
        Page<Report> expectedPage = new PageImpl<>(testReports);

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<Report> result = adminQueryService.getReportList(page, size, reportType);

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
    @DisplayName("특정 신고 유형으로 필터링된 신고 목록 조회")
    void shouldGetReportList_WithSpecificReportTypeFilter() {
        // Given
        int page = 0;
        int size = 10;
        ReportType reportType = ReportType.COMMENT;
        List<Report> commentReports = testReports.stream()
                .filter(report -> report.getReportType() == ReportType.COMMENT)
                .toList();
        Page<Report> expectedPage = new PageImpl<>(commentReports);

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(expectedPage);

        // When
        Page<Report> result = adminQueryService.getReportList(page, size, reportType);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getReportType()).isEqualTo(ReportType.COMMENT);
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), any(Pageable.class));
    }

    @Test
    @DisplayName("빈 페이지 결과 처리")
    void shouldHandleEmptyPageResult() {
        // Given
        int page = 99;
        int size = 10;
        ReportType reportType = ReportType.POST;
        Page<Report> emptyPage = new PageImpl<>(List.of());

        given(adminQueryPort.findReportsWithPaging(eq(reportType), any(Pageable.class)))
                .willReturn(emptyPage);

        // When
        Page<Report> result = adminQueryService.getReportList(page, size, reportType);

        // Then
        assertThat(result).isEqualTo(emptyPage);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(adminQueryPort).findReportsWithPaging(eq(reportType), any(Pageable.class));
    }





}