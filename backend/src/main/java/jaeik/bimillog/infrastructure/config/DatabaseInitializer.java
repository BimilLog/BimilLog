package jaeik.bimillog.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("entityManagerFactory") // EntityManagerFactory 빈이 먼저 초기화된 후 이 빈이 초기화되도록 설정
public class DatabaseInitializer {

    private final EntityManager entityManager;

    @PostConstruct
    @Transactional
    public void initializeIndexes() {
        try {
            // 'post' 테이블이 생성되었는지 확인 후 인덱스 생성 시도
            if (!checkTableExists("post")) {
                log.warn("'post' 테이블이 아직 존재하지 않습니다. 인덱스 생성을 건너뜜.");
                return; // 테이블이 없으면 인덱스 생성 시도하지 않음
            }

            boolean title = checkIndexExists("post", "idx_post_title");
            boolean title_content = checkIndexExists("post", "idx_post_title_content");

            if (!title) {
                // MySQL 8.0 이상에서 FULLTEXT 인덱스 생성
                entityManager.createNativeQuery(
                        "ALTER TABLE post ADD FULLTEXT INDEX idx_post_title (title) WITH PARSER ngram" // 'INDEX' 키워드 명시적으로 추가
                ).executeUpdate();
                log.info("글 제목 인덱스 생성 완료: idx_post_title");
            } else {
                log.debug("글 제목 인덱스가 이미 존재합니다: idx_post_title");
            }

            if (!title_content) {
                // MySQL 8.0 이상에서 FULLTEXT 인덱스 생성
                entityManager.createNativeQuery(
                        "ALTER TABLE post ADD FULLTEXT INDEX idx_post_title_content (title, content) WITH PARSER ngram" // 'INDEX' 키워드 명시적으로 추가
                ).executeUpdate();
                log.info("글 제목+내용 인덱스 생성 완료: idx_post_title_content");
            } else {
                log.debug("글 제목+내용 인덱스가 이미 존재합니다: idx_post_title_content");
            }

        } catch (Exception e) {
            log.error("인덱스 생성 실패 : {}", e.getMessage(), e); // 스택 트레이스도 로깅
        }
    }

    private boolean checkIndexExists(String tableName, String indexName) {
        try {
            // information_schema.statistics 테이블은 대소문자를 구분할 수 있으므로, 정확한 테이블 이름을 사용해야 합니다.
            // MySQL 에서는 기본적으로 대소문자를 구분하지 않지만, OS 설정에 따라 다를 수 있습니다.
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.statistics " +
                                    "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?"
                    )
                    .setParameter(1, tableName)
                    .setParameter(2, indexName)
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            log.warn("인덱스 존재 여부 확인 실패 (테이블: {}, 인덱스: {}): {}", tableName, indexName, e.getMessage());
            return false;
        }
    }

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
            log.warn("테이블 존재 여부 확인 실패 (테이블: {}): {}", tableName, e.getMessage());
            return false;
        }
    }
}