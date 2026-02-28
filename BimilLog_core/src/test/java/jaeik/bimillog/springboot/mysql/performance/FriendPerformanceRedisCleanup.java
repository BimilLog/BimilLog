package jaeik.bimillog.springboot.mysql.performance;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

/**
 * 친구 성능 테스트용 Redis 데이터 초기화 유틸리티.
 *
 * <p>DB 시드 데이터는 {@code performance-friend-rebuild.sql}로 직접 삽입하고,
 * 이 클래스는 Redis 캐시만 초기화합니다.</p>
 *
 * <pre>
 * # 1. DB 시드 삽입
 * mysql -u root -p bimillog2 < performance-friend-rebuild.sql
 *
 * # 2. Redis 초기화
 * LOCAL_MYSQL_PASSWORD=... ./gradlew localIntegrationTest --tests "*.FriendPerformanceRedisCleanup"
 * </pre>
 *
 * @author jaeik
 */
@DisplayName("친구 성능 테스트 Redis 초기화")
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@Tag("local-integration")
@Tag("performance")
@ActiveProfiles("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FriendPerformanceRedisCleanup {

    private static final Logger log = LoggerFactory.getLogger(FriendPerformanceRedisCleanup.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Redis friend/interaction 키 초기화")
    void cleanRedis() {
        log.info("=== Redis 친구/상호작용 데이터 초기화 시작 ===");

        deleteRedisByPattern("friend:*");
        deleteRedisByPattern("interaction:*");

        log.info("=== Redis 초기화 완료 ===");
    }

    private void deleteRedisByPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        log.info("  Redis 삭제 패턴={}, {}키", pattern, String.format("%,d", keys.size()));
    }
}
