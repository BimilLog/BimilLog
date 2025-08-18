package jaeik.growfarm.infrastructure.adapter.admin.out.persistence;

import jaeik.growfarm.domain.admin.entity.Report;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>AdminQueryAdapter 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 전체 컨텍스트 통합 테스트</p>
 * <p>MySQL TestContainer와 실제 QueryDSL 동작을 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:${mysql.port}/testdb",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.defer-datasource-initialization=true"
})
@Transactional
class AdminQueryAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AdminQueryAdapter adminQueryAdapter;

    @Autowired
    private EntityManager entityManager;

    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 전체 조회</h3>
     * <p>reportType이 null일 때 모든 신고를 페이지네이션하여 조회하는지 검증</p>
     */
    @Test
    void shouldReturnAllReports_WhenReportTypeIsNull() {
        // Given: 테스트 데이터 생성
        User reporter1 = createAndSaveUser("reporter1", "소셜ID1");
        User reporter2 = createAndSaveUser("reporter2", "소셜ID2");
        
        createAndSaveReport(reporter1, ReportType.POST, 1L, "게시글 신고");
        createAndSaveReport(reporter2, ReportType.COMMENT, 2L, "댓글 신고");
        createAndSaveReport(reporter1, ReportType.PAPER, 3L, "롤링페이퍼 신고");
        
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 전체 신고 목록 조회 (reportType = null)
        Page<ReportDTO> result = adminQueryAdapter.findReportsWithPaging(null, pageable);

        // Then: 모든 신고가 조회되는지 검증
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        // ReportDTO 매핑 검증
        ReportDTO firstReport = result.getContent().get(0);
        assertThat(firstReport.getReporterId()).isNotNull();
        assertThat(firstReport.getReporterName()).isNotNull();
        assertThat(firstReport.getContent()).isNotNull();
        assertThat(firstReport.getTargetId()).isNotNull();
    }

    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 특정 타입 필터링</h3>
     * <p>특정 ReportType으로 필터링하여 조회하는지 검증</p>
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
        Page<ReportDTO> result = adminQueryAdapter.findReportsWithPaging(ReportType.POST, pageable);

        // Then: POST 타입 신고만 조회되는지 검증
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(report -> report.getReportType() == ReportType.POST);
    }

    /**
     * <h3>테스트용 사용자 생성 및 저장</h3>
     */
    private User createAndSaveUser(String userName, String socialId) {
        Setting setting = Setting.builder()
                .commentNotification(true)
                .messageNotification(true)
                .postFeaturedNotification(true)
                .build();
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
    private Report createAndSaveReport(User reporter, ReportType reportType, Long targetId, String content) {
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(reportType)
                .targetId(targetId)
                .content(content)
                .build();
        
        entityManager.persist(report);
        return report;
    }
}