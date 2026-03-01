package jaeik.bimillog.springboot.mysql.performance;

import jaeik.bimillog.domain.friend.service.FriendAdminService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

/**
 * 시나리오: 회원 100,000명 / 친구 300명 / 상호작용 300명
 * 사전 조건: performance-friend-rebuild.sql 시드 데이터가 DB에 삽입되어 있어야 합니다.
 * 실행: LOCAL_MYSQL_PASSWORD=변수 gradlew localIntegrationTest --tests "*.FriendRedisRebuildPerformanceTest"
 */
@DisplayName("FriendAdminService Redis 재구축 성능 테스트")
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@Tag("local-integration")
@Tag("performance")
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FriendRedisRebuildPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(FriendRedisRebuildPerformanceTest.class);

    @Autowired
    private FriendAdminService friendAdminService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ─────────────────────────────────────────────────────────────
    //  테스트 메서드
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[성능] friendship Redis 재구축 — friend:* Set")
    void measureFriendshipRebuildTime() {
        flushFriendRedisKeys();
        log.info("");
        log.info("╔══════════════════════════════════════╗");
        log.info("║  friendship Redis 재구축 시작         ║");
        log.info("╚══════════════════════════════════════╝");

        friendAdminService.getFriendshipDB();
        log.info("▶ fire-and-forget 호출 완료 — 로그로 결과 확인");
    }

    @Test
    @Order(2)
    @DisplayName("[성능] interaction-score Redis 재구축 — interaction:* ZSet")
    void measureInteractionScoreRebuildTime() {
        flushInteractionRedisKeys();
        log.info("");
        log.info("╔══════════════════════════════════════╗");
        log.info("║  interaction-score Redis 재구축 시작  ║");
        log.info("╚══════════════════════════════════════╝");

        friendAdminService.rebuildInteractionScoreRedis();
        log.info("▶ fire-and-forget 호출 완료 — 로그로 결과 확인");
    }

    // ─────────────────────────────────────────────────────────────
    //  @AfterAll — Redis 정리
    // ─────────────────────────────────────────────────────────────
    @AfterAll
    void cleanupRedis() {
        log.info("=== Redis 정리 (friend:*, interaction:*) ===");
        flushFriendRedisKeys();
        flushInteractionRedisKeys();
    }

    // ─────────────────────────────────────────────────────────────
    //  Redis 헬퍼
    // ─────────────────────────────────────────────────────────────

    private void flushFriendRedisKeys() {
        deleteKeysByPattern("friend:*");
    }

    private void flushInteractionRedisKeys() {
        deleteKeysByPattern("interaction:*");
    }

    private void deleteKeysByPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        log.info("  Redis 삭제 패턴={}, 삭제={}키", pattern, format(keys.size()));
    }

    private long countRedisKeys(String pattern) {
        long count = 0;
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build())) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────
    //  포맷 유틸
    // ─────────────────────────────────────────────────────────────

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
