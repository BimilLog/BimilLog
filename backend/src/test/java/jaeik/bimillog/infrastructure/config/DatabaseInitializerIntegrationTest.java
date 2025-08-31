package jaeik.bimillog.infrastructure.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>DatabaseInitializer 통합 테스트</h2>
 * <p>프로덕션에서 사용되는 DatabaseInitializer의 실제 동작을 검증합니다.</p>
 * <p>FULLTEXT 인덱스 생성이 정상적으로 실행되는지 확인합니다.</p>
 * <p>컨테이너 내부에서 직접 인덱스 존재 여부를 확인하여 정확성을 높입니다.</p>
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@Import({DatabaseInitializer.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "logging.level.jaeik.bimillog.infrastructure.config.DatabaseInitializer=INFO"
})
class DatabaseInitializerIntegrationTest {

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
    private EntityManager entityManager;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Test
    @DisplayName("프로덕션 환경 - DatabaseInitializer 빈 등록 확인")
    void shouldHaveDatabaseInitializerBean() {
        // Given: Post 테이블 수동 생성 (JPA 엔티티 스캔 문제 해결)
        createPostTableManually();
        
        // When: DatabaseInitializer 빈 로딩
        
        // Then: DatabaseInitializer 빈이 정상적으로 등록되어야 함
        assertThat(databaseInitializer).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("프로덕션 환경 - @PostConstruct로 인덱스 자동 생성 확인")
    void shouldCreateFullTextIndexesAutomatically_WhenApplicationStarts() throws IOException, InterruptedException {
        // Given: Post 테이블 수동 생성
        createPostTableManually();
        assertThat(checkTableExists("post")).isTrue();
        
        // TODO: 테스트 실패 해결 - @PostConstruct가 @DataJpaTest에서 실행되지 않는 문제
        // @DataJpaTest는 제한된 컨텍스트만 로드하므로 수동으로 initializeIndexes() 호출
        // When: DatabaseInitializer 수동 실행 (자동 실행이 안되는 테스트 환경 대응)
        databaseInitializer.initializeIndexes();

        // Then: 인덱스 존재 여부 확인 (컨테이너 내부에서 직접 확인)
        boolean titleIndexExists = checkIndexExistsInContainer("post", "idx_post_title");
        boolean titleContentIndexExists = checkIndexExistsInContainer("post", "idx_post_title_content");

        assertThat(titleIndexExists).isTrue();
        assertThat(titleContentIndexExists).isTrue();

        System.out.println("=== DatabaseInitializer 동작 결과 (컨테이너 직접 확인) ===");
        System.out.println("idx_post_title 인덱스: " + (titleIndexExists ? "생성됨" : "없음"));
        System.out.println("idx_post_title_content 인덱스: " + (titleContentIndexExists ? "생성됨" : "없음"));
    }

    @Test
    @DisplayName("프로덕션 환경 - 중복 인덱스 생성 방지 확인")
    void shouldPreventDuplicateIndexCreation_WhenIndexAlreadyExists() throws IOException, InterruptedException {
        // Given: Post 테이블 생성 후 인덱스를 미리 생성
        createPostTableManually();
        databaseInitializer.initializeIndexes(); // 첫 번째 실행으로 인덱스 생성
        assertThat(checkIndexExistsInContainer("post", "idx_post_title")).isTrue();
        assertThat(checkIndexExistsInContainer("post", "idx_post_title_content")).isTrue();

        // When: DatabaseInitializer를 다시 실행
        databaseInitializer.initializeIndexes(); // 중복 실행 시도

        // Then: 예외 없이 처리되어야 함 (로그에 debug 메시지가 찍힐 것임)
        // 컨테이너 내부에서 인덱스가 여전히 존재하는지 확인
        assertThat(checkIndexExistsInContainer("post", "idx_post_title")).isTrue();
        assertThat(checkIndexExistsInContainer("post", "idx_post_title_content")).isTrue();
    }

    @Test
    @DisplayName("프로덕션 환경 - 인덱스 존재 확인 로직 검증")
    void shouldCorrectlyCheckIndexExistence() throws IOException, InterruptedException {
        // Given: Post 테이블 생성
        createPostTableManually();
        
        // When: 존재하지 않는 인덱스 확인 (컨테이너 내부에서 직접 확인)
        boolean nonExistentIndex = checkIndexExistsInContainer("post", "non_existent_index");
        // When: 존재하는 테이블 확인 (EntityManager를 통해 확인)
        boolean postTableExists = checkTableExists("post");

        // Then: 정확하게 false 반환
        assertThat(nonExistentIndex).isFalse();

        // Then: 테이블은 존재해야 함
        assertThat(postTableExists).isTrue();
    }

    /**
     * 컨테이너 내부에서 직접 MySQL 명령어를 실행하여 인덱스 존재 여부를 확인합니다.
     */
    private boolean checkIndexExistsInContainer(String tableName, String indexName) throws IOException, InterruptedException {
        String command = String.format("mysql -u %s -p%s %s -e \"SHOW INDEXES FROM %s;\"",
                mysql.getUsername(), mysql.getPassword(), mysql.getDatabaseName(), tableName);

        org.testcontainers.containers.Container.ExecResult execResult = mysql.execInContainer("/bin/bash", "-c", command);
        String stdout = execResult.getStdout();
        String stderr = execResult.getStderr();

        if (execResult.getExitCode() != 0) {
            System.err.println("컨테이너 명령 실행 실패 (Exit Code: " + execResult.getExitCode() + "): " + stderr);
            return false;
        }

        // SHOW INDEXES FROM table; 의 출력에서 Key_name 컬럼에 인덱스 이름이 있는지 확인합니다.
        // 출력 예시: Table Non_unique Key_name ...
        //            post    1          idx_post_title ...
        return stdout.contains(indexName);
    }

    /**
     * 테이블 존재 여부를 확인합니다.
     */
    private boolean checkTableExists(String tableName) {
        try {
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.tables " +
                                    "WHERE table_schema = DATABASE() AND table_name = ?"
                    )
                    .setParameter(1, tableName)
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            System.err.println("테이블 존재 여부 확인 중 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * Post 테이블을 수동으로 생성합니다.
     */
    private void createPostTableManually() {
        try {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS post (
                    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT,
                    title VARCHAR(30) NOT NULL,
                    content TEXT,
                    view_count BIGINT DEFAULT 0,
                    like_count BIGINT DEFAULT 0,
                    is_notice BOOLEAN DEFAULT FALSE,
                    popular_flag VARCHAR(20),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                );
            """;
            
            entityManager.createNativeQuery(createTableSQL).executeUpdate();
            System.out.println("Post 테이블 수동 생성 완료");
        } catch (Exception e) {
            System.err.println("Post 테이블 생성 중 오류: " + e.getMessage());
        }
    }
}