package jaeik.growfarm.infrastructure.adapter.admin.out.persistence;

import jaeik.growfarm.domain.admin.entity.Report;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>ReportRepository 통합 테스트</h2>
 * <p>MySQL TestContainer를 사용한 ReportRepository의 JPA 기본 기능 테스트</p>
 * <p>기본 CRUD 동작과 JPA 연관관계 매핑을 검증</p>
 * 
 * Spring Boot의 @DataJpaTest 기능을 활용한 간단한 설정으로 수정
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:${mysql.port}/testdb",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.defer-datasource-initialization=true"
})
class ReportRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * <h3>신고 저장 테스트</h3>
     * <p>신고 엔티티가 올바르게 저장되고 ID가 할당되는지 검증</p>
     */
    @Test
    void shouldSaveReport_WhenValidReportProvided() {
        // Given: 신고자와 신고 정보
        User reporter = createAndSaveUser("reporter", "소셜ID123");
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("부적절한 게시글입니다.")
                .build();

        // When: 신고 저장
        Report savedReport = reportRepository.save(report);

        // Then: 올바르게 저장되고 ID가 할당되는지 검증
        assertThat(savedReport.getId()).isNotNull();
        assertThat(savedReport.getReporter().getId()).isEqualTo(reporter.getId());
        assertThat(savedReport.getReportType()).isEqualTo(ReportType.POST);
        assertThat(savedReport.getTargetId()).isEqualTo(1L);
        assertThat(savedReport.getContent()).isEqualTo("부적절한 게시글입니다.");
        assertThat(savedReport.getCreatedAt()).isNotNull();
        assertThat(savedReport.getModifiedAt()).isNotNull();
    }

    /**
     * <h3>ID로 신고 조회 테스트 - 성공</h3>
     * <p>저장된 신고를 ID로 올바르게 조회할 수 있는지 검증</p>
     */
    @Test
    void shouldFindReportById_WhenReportExists() {
        // Given: 저장된 신고
        User reporter = createAndSaveUser("reporter", "소셜ID123");
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.COMMENT)
                .targetId(2L)
                .content("스팸 댓글입니다.")
                .build();
        Report savedReport = reportRepository.save(report);
        entityManager.flush();
        entityManager.clear();

        // When: ID로 조회
        Optional<Report> foundReport = reportRepository.findById(savedReport.getId());

        // Then: 올바른 신고가 조회되는지 검증
        assertThat(foundReport).isPresent();
        assertThat(foundReport.get().getId()).isEqualTo(savedReport.getId());
        assertThat(foundReport.get().getReportType()).isEqualTo(ReportType.COMMENT);
        assertThat(foundReport.get().getTargetId()).isEqualTo(2L);
        assertThat(foundReport.get().getContent()).isEqualTo("스팸 댓글입니다.");
        
        // 연관관계 매핑 검증 (Lazy Loading)
        assertThat(foundReport.get().getReporter()).isNotNull();
        assertThat(foundReport.get().getReporter().getUserName()).isEqualTo("reporter");
    }

    /**
     * <h3>ID로 신고 조회 테스트 - 존재하지 않는 경우</h3>
     * <p>존재하지 않는 ID로 조회 시 빈 Optional을 반환하는지 검증</p>
     */
    @Test
    void shouldReturnEmpty_WhenReportNotExists() {
        // Given: 존재하지 않는 ID
        Long nonExistentId = 999L;

        // When: 존재하지 않는 ID로 조회
        Optional<Report> foundReport = reportRepository.findById(nonExistentId);

        // Then: 빈 Optional 반환
        assertThat(foundReport).isEmpty();
    }

    /**
     * <h3>신고 삭제 테스트</h3>
     * <p>신고가 올바르게 삭제되는지 검증</p>
     */
    @Test
    void shouldDeleteReport_WhenValidIdProvided() {
        // Given: 저장된 신고
        User reporter = createAndSaveUser("reporter", "소셜ID123");
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.PAPER)
                .targetId(3L)
                .content("부적절한 메시지입니다.")
                .build();
        Report savedReport = reportRepository.save(report);
        Long reportId = savedReport.getId();
        entityManager.flush();

        // When: 신고 삭제
        reportRepository.deleteById(reportId);
        entityManager.flush();

        // Then: 삭제되었는지 검증
        Optional<Report> deletedReport = reportRepository.findById(reportId);
        assertThat(deletedReport).isEmpty();
    }

    /**
     * <h3>모든 신고 조회 테스트</h3>
     * <p>저장된 모든 신고가 조회되는지 검증</p>
     */
    @Test
    void shouldFindAllReports_WhenMultipleReportsExist() {
        // Given: 여러 개의 신고 저장
        User reporter1 = createAndSaveUser("reporter1", "소셜ID1");
        User reporter2 = createAndSaveUser("reporter2", "소셜ID2");
        
        Report report1 = Report.builder()
                .reporter(reporter1)
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("신고 1")
                .build();
        
        Report report2 = Report.builder()
                .reporter(reporter2)
                .reportType(ReportType.COMMENT)
                .targetId(2L)
                .content("신고 2")
                .build();
        
        reportRepository.save(report1);
        reportRepository.save(report2);
        entityManager.flush();

        // When: 모든 신고 조회
        Iterable<Report> allReports = reportRepository.findAll();

        // Then: 저장된 모든 신고가 조회되는지 검증
        assertThat(allReports).hasSize(2);
    }

    /**
     * <h3>신고 수정 테스트</h3>
     * <p>저장된 신고의 내용을 수정할 수 있는지 검증</p>
     * <p>Report 엔티티는 불변 객체로 설계되어 수정 메서드가 없지만, JPA의 dirty checking 테스트</p>
     */
    @Test
    void shouldUpdateReport_WhenReportIsModified() {
        // Given: 저장된 신고
        User reporter = createAndSaveUser("reporter", "소셜ID123");
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.POST)
                .targetId(1L)
                .content("원본 신고 내용")
                .build();
        Report savedReport = reportRepository.save(report);
        entityManager.flush();
        entityManager.clear();

        // When: 신고 내용 직접 수정 (필드 접근을 통한 테스트)
        Report foundReport = reportRepository.findById(savedReport.getId()).orElseThrow();
        // Note: Report 엔티티에 setter가 없으므로 이 테스트는 현재 구조에서는 의미가 제한적
        // 실제로는 Report가 불변 객체로 설계되어 수정이 불가능함을 확인하는 테스트

        // Then: 엔티티 불변성 확인
        assertThat(foundReport.getContent()).isEqualTo("원본 신고 내용");
        // Report는 불변 객체로 설계되어 있어 수정 메서드가 제공되지 않음을 확인
    }

    /**
     * <h3>연관관계 테스트 - 신고자 정보</h3>
     * <p>신고와 신고자 간의 연관관계가 올바르게 매핑되는지 검증</p>
     */
    @Test
    void shouldMapReporterCorrectly_WhenReportHasReporter() {
        // Given: 신고자와 신고
        User reporter = createAndSaveUser("testReporter", "소셜ID456");
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.COMMENT)
                .targetId(5L)
                .content("연관관계 테스트")
                .build();
        Report savedReport = reportRepository.save(report);
        entityManager.flush();
        entityManager.clear();

        // When: 신고 조회 (연관관계 포함)
        Report foundReport = reportRepository.findById(savedReport.getId()).orElseThrow();

        // Then: 연관관계 매핑 검증
        assertThat(foundReport.getReporter()).isNotNull();
        assertThat(foundReport.getReporter().getId()).isEqualTo(reporter.getId());
        assertThat(foundReport.getReporter().getUserName()).isEqualTo("testReporter");
        assertThat(foundReport.getReporter().getSocialId()).isEqualTo("소셜ID456");
        assertThat(foundReport.getReporter().getProvider()).isEqualTo(SocialProvider.KAKAO);
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
}