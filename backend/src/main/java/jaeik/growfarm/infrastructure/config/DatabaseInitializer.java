package jaeik.growfarm.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {
    
    private final EntityManager entityManager;
    
    @PostConstruct
    public void initializeIndexes() {
        try {
            boolean title = checkIndexExists("post", "idx_post_title");
            boolean title_content = checkIndexExists("post", "idx_post_title_content");

            if (!title) {
                entityManager.createNativeQuery(
                    "ALTER TABLE post ADD FULLTEXT idx_post_title (title) WITH PARSER ngram"
                ).executeUpdate();
                log.info("글 제목 인덱스 생성 완료");
            } else {
                log.debug("글 제목 인덱스가 이미 존재합니다");
            }

            if (!title_content) {
                entityManager.createNativeQuery(
                        "ALTER TABLE post ADD FULLTEXT idx_post_title_content (title, content) WITH PARSER ngram"
                ).executeUpdate();
                log.info("글 제목+내용 인덱스 생성 완료");
            } else {
                log.debug("글 제목+내용 인덱스가 이미 존재합니다");
            }

        } catch (Exception e) {
            log.error("인덱스 생성 실패 : {}", e.getMessage());
        }
    }
    
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
            log.warn("인덱스 존재 여부 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}