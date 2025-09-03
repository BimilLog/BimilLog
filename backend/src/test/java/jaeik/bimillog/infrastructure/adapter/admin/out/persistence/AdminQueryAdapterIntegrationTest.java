package jaeik.bimillog.infrastructure.adapter.admin.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.admin.entity.ReportSummary;
import jaeik.bimillog.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.bimillog.domain.admin.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.paper.entity",
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.comment.entity",
        "jaeik.bimillog.domain.notification.entity",
        "jaeik.bimillog.domain.common.entity"
})
@Import(AdminQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class AdminQueryAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }

        // EncryptionUtil 빈 정의: MessageEncryptConverter의 의존성을 만족시킵니다.
        @Bean
        public EncryptionUtil encryptionUtil() {
            // 테스트를 위해 간단한 더미 인스턴스를 반환합니다.
            // 실제 EncryptionUtil이 복잡한 의존성을 가진다면 Mockito.mock(EncryptionUtil.class)를 사용할 수 있습니다.
            return new EncryptionUtil();
        }
    }

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
        Page<ReportSummary> result = adminQueryAdapter.findReportsWithPaging(null, pageable);

        // Then: 모든 신고가 조회되는지 검증
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);

        // ReportDTO 매핑 검증
        ReportSummary firstReport = result.getContent().getFirst();
        assertThat(firstReport.reporterId()).isNotNull();
        assertThat(firstReport.reporterName()).isNotNull();
        assertThat(firstReport.content()).isNotNull();
        assertThat(firstReport.targetId()).isNotNull();
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
        Page<ReportSummary> result = adminQueryAdapter.findReportsWithPaging(ReportType.POST, pageable);

        // Then: POST 타입 신고만 조회되는지 검증
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(report -> report.reportType() == ReportType.POST);
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
