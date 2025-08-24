package jaeik.growfarm.infrastructure.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * @version 2.1.0
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=true",
        "logging.level.jaeik.growfarm.infrastructure.config.DatabaseInitializer=INFO"
})
@Transactional
class DatabaseInitializerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withEnv("MYSQL_INITDB_SKIP_TZINFO", "true");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Test
    @DisplayName("프로덕션 환경 - DatabaseInitializer 빈 등록 확인")
    void shouldHaveDatabaseInitializerBean() {
        assertThat(databaseInitializer).isNotNull();
    }

    @Test
    @DisplayName("프로덕션 환경 - @PostConstruct로 인덱스 자동 생성 확인")
    void shouldCreateFullTextIndexesAutomatically_WhenApplicationStarts() throws IOException, InterruptedException { // 예외 선언 추가
        // Given: Spring Boot 시작 시 @PostConstruct 실행됨 (컨텍스트 로딩 시에 실행되었음)
        // DDL-auto=create에 의해 Post 테이블이 생성되었는지 확인
        assertThat(checkTableExists("post")).isTrue();

        // When/Then: 인덱스 존재 여부 확인 (컨테이너 내부에서 직접 확인)
        boolean titleIndexExists = checkIndexExistsInContainer("post", "idx_post_title");
        boolean titleContentIndexExists = checkIndexExistsInContainer("post", "idx_post_title_content");

        assertThat(titleIndexExists).isTrue();
        assertThat(titleContentIndexExists).isTrue();

        System.out.println("=== DatabaseInitializer 동작 결과 (컨테이너 직접 확인) ===");
        System.out.println("idx_post_title 인덱스: " + (titleIndexExists ? "생성됨" : "없음"));
        System.out.println("idx_post_title_content 인덱스: " + (titleContentIndexExists ? "생성됨" : "없음"));
    }

    @Test
    @DisplayName("프로덕션 환경 - 수동 인덱스 생성 시도 (인덱스 미존재 시 생성)")
    void shouldAttemptIndexCreation_WhenManuallyTriggered() throws IOException, InterruptedException { // 예외 선언 추가
        // Given: 기존 인덱스 삭제 (수동 테스트를 위해)
        removeIndexIfExistsInContainer("post", "idx_post_title");
        removeIndexIfExistsInContainer("post", "idx_post_title_content");

        // When: DatabaseInitializer 수동 실행
        databaseInitializer.initializeIndexes();

        // Then: 인덱스 생성 확인 (컨테이너 내부에서 직접 확인)
        assertThat(checkIndexExistsInContainer("post", "idx_post_title")).isTrue();
        assertThat(checkIndexExistsInContainer("post", "idx_post_title_content")).isTrue();
    }

    @Test
    @DisplayName("프로덕션 환경 - 중복 인덱스 생성 방지 확인")
    void shouldPreventDuplicateIndexCreation_WhenIndexAlreadyExists() throws IOException, InterruptedException { // 예외 선언 추가
        // Given: 인덱스가 이미 존재하는 상황 시뮬레이션 (@PostConstruct에서 이미 생성되었거나 이전 테스트에서 생성)
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
    void shouldCorrectlyCheckIndexExistence() throws IOException, InterruptedException { // 예외 선언 추가
        // When: 존재하지 않는 인덱스 확인 (컨테이너 내부에서 직접 확인)
        boolean nonExistentIndex = checkIndexExistsInContainer("post", "non_existent_index");

        // Then: 정확하게 false 반환
        assertThat(nonExistentIndex).isFalse();

        // When: 존재하는 테이블 확인 (EntityManager를 통해 확인)
        boolean postTableExists = checkTableExists("post");

        // Then: 테이블은 존재해야 함
        assertThat(postTableExists).isTrue();
    }

    /**
     * 인덱스 존재 여부를 확인합니다. (EntityManager 사용)
     * 이 메서드는 DatabaseInitializer 내부 로직에서 사용되는 것과 동일합니다.
     */
    private boolean checkIndexExists(String tableName, String indexName) {
        try {
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.statistics " +
                                    "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?"
                    )
                    .setParameter(1, tableName)
                    .setParameter(2, indexName)
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            System.err.println("인덱스 존재 여부 확인 중 오류 (EntityManager): " + e.getMessage());
            return false;
        }
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
     * 테스트를 위해 컨테이너 내부에서 직접 인덱스를 삭제합니다.
     */
    private void removeIndexIfExistsInContainer(String tableName, String indexName) throws IOException, InterruptedException {
        if (checkIndexExistsInContainer(tableName, indexName)) {
            String command = String.format("mysql -u %s -p%s %s -e \"ALTER TABLE %s DROP INDEX %s;\"",
                    mysql.getUsername(), mysql.getPassword(), mysql.getDatabaseName(), tableName, indexName);
            org.testcontainers.containers.Container.ExecResult execResult = mysql.execInContainer("/bin/bash", "-c", command);

            if (execResult.getExitCode() == 0) {
                System.out.println("컨테이너 인덱스 삭제 완료: " + indexName);
            } else {
                System.err.println("컨테이너 인덱스 삭제 실패 (Exit Code: " + execResult.getExitCode() + "): " + execResult.getStderr());
            }
        }
    }
}