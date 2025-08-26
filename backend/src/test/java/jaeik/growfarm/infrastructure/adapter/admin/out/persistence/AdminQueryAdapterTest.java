package jaeik.growfarm.infrastructure.adapter.admin.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.admin.entity.Report;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.domain.admin.entity.ReportSummary;
import jaeik.growfarm.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>AdminQueryAdapter 통합 테스트</h2>
 * <p>MySQL TestContainer를 사용한 신고 조회 어댑터의 데이터베이스 통합 테스트</p>
 * <p>QueryDSL과 JPA Repository의 실제 동작을 검증</p>
 * 
 * Spring Boot의 @DataJpaTest 기능을 활용한 간단한 설정으로 수정
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GrowfarmApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.admin.entity", 
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.paper.entity",
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.comment.entity",
        "jaeik.growfarm.domain.notification.entity",
        "jaeik.growfarm.domain.common.entity"
})
@Import(AdminQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class AdminQueryAdapterTest {

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
    private TestEntityManager entityManager;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AdminQueryAdapter adminQueryAdapter;


    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 전체 조회</h3>
     * <p>reportType이 null일 때 모든 신고를 페이지네이션하여 조회하는지 검증</p>
     */
    @Test
    void shouldReturnAllReports_WhenReportTypeIsNull() {
        // Given: 테스트 데이터 생성
        User reporter1 = createAndSaveUser("reporter1", "소셜ID1");
        User reporter2 = createAndSaveUser("reporter2", "소셜ID2");
        
        Report postReport = createAndSaveReport(reporter1, ReportType.POST, 1L, "게시글 신고");
        Report commentReport = createAndSaveReport(reporter2, ReportType.COMMENT, 2L, "댓글 신고");
        Report paperReport = createAndSaveReport(reporter1, ReportType.PAPER, 3L, "롤링페이퍼 신고");
        
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 전체 신고 목록 조회 (reportType = null)
        Page<ReportSummary> result = adminQueryAdapter.findReportsWithPaging(null, pageable);

        // Then: 모든 신고가 조회되는지 검증 (정렬 순서는 ID 기준으로 검증)
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        // 정렬 검증은 ID 기준으로 변경 (시간은 마이크로초 차이로 불안정함)
        assertThat(result.getContent().get(0).id()).isGreaterThan(result.getContent().get(1).id());
        assertThat(result.getContent().get(1).id()).isGreaterThan(result.getContent().get(2).id());
        
        // ReportDTO 매핑 검증 (마지막으로 생성된 것이 첫 번째로 와야 함)
        ReportSummary firstReport = result.getContent().get(0);
        assertThat(firstReport.reporterId()).isNotNull();
        assertThat(firstReport.reporterName()).isNotNull();
        assertThat(firstReport.content()).isNotNull();
        assertThat(firstReport.targetId()).isNotNull();
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
        Page<ReportSummary> result = adminQueryAdapter.findReportsWithPaging(ReportType.POST, pageable);

        // Then: POST 타입 신고만 조회되는지 검증
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(report -> report.reportType() == ReportType.POST);
    }

    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 빈 결과</h3>
     * <p>조건에 맞는 신고가 없을 때 빈 페이지를 반환하는지 검증</p>
     */
    @Test
    void shouldReturnEmptyPage_WhenNoReportsMatchFilter() {
        // Given: COMMENT 타입만 있는 상황
        User reporter = createAndSaveUser("reporter", "소셜ID");
        createAndSaveReport(reporter, ReportType.COMMENT, 1L, "댓글 신고");
        
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: PAPER 타입으로 조회
        Page<ReportSummary> result = adminQueryAdapter.findReportsWithPaging(ReportType.PAPER, pageable);

        // Then: 빈 결과 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    /**
     * <h3>신고 목록 페이지네이션 조회 테스트 - 페이징 동작</h3>
     * <p>페이징이 올바르게 동작하는지 검증</p>
     */
    @Test
    void shouldReturnCorrectPage_WhenPagingIsApplied() {
        // Given: 5개의 신고 데이터 생성
        User reporter = createAndSaveUser("reporter", "소셜ID");
        
        for (int i = 1; i <= 5; i++) {
            createAndSaveReport(reporter, ReportType.POST, (long) i, "신고 " + i);
        }
        
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 크기 2

        // When: 두 번째 페이지 조회
        Page<ReportSummary> result = adminQueryAdapter.findReportsWithPaging(null, pageable);

        // Then: 페이징 정보 검증
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1); // 현재 페이지
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
    }

    /**
     * <h3>ID로 신고 조회 테스트 - 존재하는 경우</h3>
     * <p>존재하는 신고 ID로 조회 시 올바른 엔티티를 반환하는지 검증</p>
     */
    @Test
    void shouldReturnReport_WhenReportExists() {
        // Given: 신고 데이터 생성
        User reporter = createAndSaveUser("reporter", "소셜ID");
        Report savedReport = createAndSaveReport(reporter, ReportType.POST, 1L, "테스트 신고");
        
        entityManager.flush();
        entityManager.clear();

        // When: ID로 신고 조회
        Optional<Report> result = adminQueryAdapter.findById(savedReport.getId());

        // Then: 올바른 신고가 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedReport.getId());
        assertThat(result.get().getContent()).isEqualTo("테스트 신고");
        assertThat(result.get().getReportType()).isEqualTo(ReportType.POST);
        assertThat(result.get().getTargetId()).isEqualTo(1L);
    }

    /**
     * <h3>ID로 신고 조회 테스트 - 존재하지 않는 경우</h3>
     * <p>존재하지 않는 신고 ID로 조회 시 빈 Optional을 반환하는지 검증</p>
     */
    @Test
    void shouldReturnEmpty_WhenReportDoesNotExist() {
        // Given: 존재하지 않는 신고 ID
        Long nonExistentId = 999L;

        // When: 존재하지 않는 ID로 조회
        Optional<Report> result = adminQueryAdapter.findById(nonExistentId);

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
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
        
        return entityManager.persistAndFlush(user);
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
        
        return entityManager.persistAndFlush(report);
    }
}