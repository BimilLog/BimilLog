package jaeik.bimillog.infrastructure.adapter.out.admin;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSettings;
import jaeik.bimillog.testutil.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@Import({AdminQueryAdapter.class, TestContainersConfiguration.class})
class AdminQueryAdapterIntegrationTest {

    @Autowired
    private AdminQueryAdapter adminQueryAdapter;

    @Autowired
    private EntityManager entityManager;



    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 전체 조회</h3>
     */
    @Test
    void shouldReturnAllReports_WhenReportTypeIsNull() {
        // Given: 테스트 데이터 생성
        User reporter1 = createAndSaveUser("reporter1", "소셜ID1");
        User reporter2 = createAndSaveUser("reporter2", "소셜ID2");

        createAndSaveReport(reporter1, ReportType.POST, 1L, "게시글 신고");
        createAndSaveReport(reporter2, ReportType.COMMENT, 2L, "댓글 신고");
        createAndSaveReport(reporter1, ReportType.IMPROVEMENT, 3L, "롤링페이퍼 신고");

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 전체 신고 목록 조회 (reportType = null)
        Page<Report> result = adminQueryAdapter.findReportsWithPaging(null, pageable);

        // Then: 모든 신고가 조회되는지 검증
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);

        // ReportDTO 매핑 검증
        Report firstReport = result.getContent().getFirst();
        assertThat(firstReport.getReporter().getId()).isNotNull();
        assertThat(firstReport.getReporter().getUserName()).isNotNull();
        assertThat(firstReport.getContent()).isNotNull();
        assertThat(firstReport.getTargetId()).isNotNull();
    }

    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 특정 타입 필터링</h3>
     */
    @Test
    void shouldReturnFilteredReports_WhenReportTypeIsSpecified() {
        // Given: 다양한 타입의 신고 데이터 생성
        User reporter = createAndSaveUser("reporter", "소셜ID");

        createAndSaveReport(reporter, ReportType.POST, 1L, "게시글 신고 1");
        createAndSaveReport(reporter, ReportType.POST, 2L, "게시글 신고 2");
        createAndSaveReport(reporter, ReportType.COMMENT, 3L, "댓글 신고");

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: POST 타입만 조회
        Page<Report> result = adminQueryAdapter.findReportsWithPaging(ReportType.POST, pageable);

        // Then: POST 타입 신고만 조회되는지 검증
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(report -> report.getReportType() == ReportType.POST);
    }

    /**
     * <h3>테스트용 사용자 생성 및 저장</h3>
     */
    private User createAndSaveUser(String userName, String socialId) {
        Setting setting = TestSettings.copy(TestSettings.DEFAULT);
        entityManager.persist(setting);

        User user = User.builder()
                .userName(userName)
                .socialId(socialId)
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .socialNickname(userName + "_nickname")
                .setting(setting)
                .build();

        entityManager.persist(user);
        return user;
    }

    /**
     * <h3>테스트용 신고 생성 및 저장</h3>
     */
    private void createAndSaveReport(User reporter, ReportType reportType, Long targetId, String content) {
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(reportType)
                .targetId(targetId)
                .content(content)
                .build();

        entityManager.persist(report);
    }
}
