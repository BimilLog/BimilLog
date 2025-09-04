package jaeik.bimillog.infrastructure.adapter.admin.out.persistence;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>AdminCommandAdapter 단위 테스트</h2>
 * <p>AdminCommandAdapter의 데이터 저장 로직에 대한 단위 테스트</p>
 * <p>Given-When-Then 패턴을 사용하여 테스트를 구조화합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class AdminCommandAdapterTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AdminCommandAdapter adminCommandAdapter;

    @Test
    @DisplayName("인증된 사용자 신고 저장 - 성공")
    void save_AuthenticatedUserReport_Success() {
        // Given
        User reporter = User.builder()
                .id(1L)
                .userName("testuser")
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .build();

        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "부적절한 댓글입니다";
        Report inputReport = Report.createReport(reportType, targetId, content, reporter);
        
        Report savedReport = Report.createReport(reportType, targetId, content, reporter);
        savedReport = Report.builder()
                .id(100L) // 저장 후 ID 할당됨
                .reporter(reporter)
                .reportType(ReportType.COMMENT)
                .targetId(123L)
                .content("부적절한 댓글입니다")
                .build();

        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = adminCommandAdapter.save(inputReport);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getReporter()).isEqualTo(reporter);
        assertThat(result.getReportType()).isEqualTo(ReportType.COMMENT);
        assertThat(result.getTargetId()).isEqualTo(123L);
        assertThat(result.getContent()).isEqualTo("부적절한 댓글입니다");

        verify(reportRepository, times(1)).save(inputReport);
    }

    @Test
    @DisplayName("익명 사용자 신고 저장 - 성공")
    void save_AnonymousUserReport_Success() {
        // Given
        ReportType reportType = ReportType.POST;
        Long targetId = 456L;
        String content = "스팸 게시글입니다";
        Report inputReport = Report.createReport(reportType, targetId, content, null); // 익명 사용자

        Report savedReport = Report.builder()
                .id(101L) // 저장 후 ID 할당됨
                .reporter(null) // 익명 사용자
                .reportType(ReportType.POST)
                .targetId(456L)
                .content("스팸 게시글입니다")
                .build();

        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = adminCommandAdapter.save(inputReport);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getReporter()).isNull(); // 익명 사용자
        assertThat(result.getReportType()).isEqualTo(ReportType.POST);
        assertThat(result.getTargetId()).isEqualTo(456L);
        assertThat(result.getContent()).isEqualTo("스팸 게시글입니다");

        verify(reportRepository, times(1)).save(inputReport);
    }

    @Test
    @DisplayName("건의사항 저장 - 성공 (targetId null)")
    void save_Suggestion_Success() {
        // Given
        User reporter = User.builder()
                .id(2L)
                .userName("suggester")
                .socialId("social456")
                .provider(SocialProvider.KAKAO)
                .build();

        ReportType reportType = ReportType.IMPROVEMENT;
        Long targetId = null;
        String content = "새로운 기능을 건의합니다";
        Report inputReport = Report.createReport(reportType, targetId, content, reporter);

        Report savedReport = Report.builder()
                .id(102L) // 저장 후 ID 할당됨
                .reporter(reporter)
                .reportType(ReportType.IMPROVEMENT)
                .targetId(null) // 건의사항은 targetId가 null
                .content("새로운 기능을 건의합니다")
                .build();

        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = adminCommandAdapter.save(inputReport);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(102L);
        assertThat(result.getReporter()).isEqualTo(reporter);
        assertThat(result.getReportType()).isEqualTo(ReportType.IMPROVEMENT);
        assertThat(result.getTargetId()).isNull(); // 건의사항은 targetId가 null
        assertThat(result.getContent()).isEqualTo("새로운 기능을 건의합니다");

        verify(reportRepository, times(1)).save(inputReport);
    }

    @Test
    @DisplayName("Repository 호출 횟수 검증")
    void save_VerifyRepositoryCallCount() {
        // Given
        ReportType reportType = ReportType.COMMENT;
        Long targetId = 123L;
        String content = "테스트 신고";
        Report inputReport = Report.createReport(reportType, targetId, content, null);
        
        Report savedReport = Report.builder()
                .id(103L)
                .reporter(null)
                .reportType(ReportType.COMMENT)
                .targetId(123L)
                .content("테스트 신고")
                .build();

        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        adminCommandAdapter.save(inputReport);

        // Then
        verify(reportRepository, times(1)).save(inputReport);
        verify(reportRepository, only()).save(inputReport);
    }

    @Test
    @DisplayName("여러 신고 연속 저장 - 성공")
    void save_MultipleReports_Success() {
        // Given
        ReportType reportType1 = ReportType.COMMENT;
        ReportType reportType2 = ReportType.POST;
        ReportType reportType3 = ReportType.IMPROVEMENT;
        Long targetId1 = 100L;
        Long targetId2 = 200L;
        Long targetId3 = null;
        String content1 = "신고 내용 1";
        String content2 = "신고 내용 2";
        String content3 = "건의 내용";

        Report report1 = Report.createReport(reportType1, targetId1, content1, null);
        Report report2 = Report.createReport(reportType2, targetId2, content2, null);
        Report report3 = Report.createReport(reportType3, targetId3, content3, null);

        Report savedReport1 = Report.builder().id(104L).reportType(ReportType.COMMENT).build();
        Report savedReport2 = Report.builder().id(105L).reportType(ReportType.POST).build();
        Report savedReport3 = Report.builder().id(106L).reportType(ReportType.IMPROVEMENT).build();

        when(reportRepository.save(report1)).thenReturn(savedReport1);
        when(reportRepository.save(report2)).thenReturn(savedReport2);
        when(reportRepository.save(report3)).thenReturn(savedReport3);

        // When
        Report result1 = adminCommandAdapter.save(report1);
        Report result2 = adminCommandAdapter.save(report2);
        Report result3 = adminCommandAdapter.save(report3);

        // Then
        assertThat(result1.getId()).isEqualTo(104L);
        assertThat(result2.getId()).isEqualTo(105L);
        assertThat(result3.getId()).isEqualTo(106L);

        verify(reportRepository, times(3)).save(any(Report.class));
        verify(reportRepository, times(1)).save(report1);
        verify(reportRepository, times(1)).save(report2);
        verify(reportRepository, times(1)).save(report3);
    }

    @Test
    @DisplayName("Repository가 반환한 결과를 그대로 반환")
    void save_ReturnRepositoryResult() {
        // Given
        ReportType reportType = ReportType.POST;
        Long targetId = 999L;
        String content = "테스트 신고 내용";
        Report inputReport = Report.createReport(reportType, targetId, content, null);

        // Repository가 반환할 특정 Report 객체
        Report expectedReport = Report.builder()
                .id(888L)
                .reporter(null)
                .reportType(ReportType.POST)
                .targetId(999L)
                .content("테스트 신고 내용")
                .build();

        when(reportRepository.save(inputReport)).thenReturn(expectedReport);

        // When
        Report actualReport = adminCommandAdapter.save(inputReport);

        // Then
        assertThat(actualReport).isSameAs(expectedReport); // 동일한 객체 참조인지 확인
        assertThat(actualReport).isEqualTo(expectedReport); // 내용이 같은지 확인
    }
}