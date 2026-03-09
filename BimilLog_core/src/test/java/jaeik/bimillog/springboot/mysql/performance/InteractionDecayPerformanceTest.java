package jaeik.bimillog.springboot.mysql.performance;

import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static jaeik.bimillog.infrastructure.redis.RedisKey.createInteractionKey;

/**
 * <h2>상호작용 점수 지수 감쇠 Lua 스크립트 성능 테스트</h2>
 * <p>다양한 규모의 시드 데이터로 감쇠 실행 시간을 측정합니다.</p>
 * <p>실행: LOCAL_MYSQL_PASSWORD=변수 gradlew localIntegrationTest --tests "*.InteractionDecayPerformanceTest"</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@DisplayName("상호작용 점수 지수 감쇠 성능 테스트")
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
class InteractionDecayPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(InteractionDecayPerformanceTest.class);

    @Autowired
    private RedisInteractionScoreRepository redisInteractionScoreRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @AfterAll
    void cleanupRedis() {
        flushInteractionKeys();
    }

    @Test
    @Order(1)
    @DisplayName("[성능] 회원 1,000명 x 멤버 50명 — 감쇠 실행 시간")
    void decayPerformance_1000keys_50members() {
        runDecayTest(1_000, 50);
    }

    @Test
    @Order(2)
    @DisplayName("[성능] 회원 5,000명 x 멤버 50명 — 감쇠 실행 시간")
    void decayPerformance_5000keys_50members() {
        runDecayTest(5_000, 50);
    }

    @Test
    @Order(3)
    @DisplayName("[성능] 회원 10,000명 x 멤버 100명 — 감쇠 실행 시간")
    void decayPerformance_10000keys_100members() {
        runDecayTest(10_000, 100);
    }

    private void runDecayTest(int keyCount, int membersPerKey) {
        flushInteractionKeys();

        // 시드 데이터 생성
        log.info("");
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  감쇠 성능 테스트: {}키 x {}멤버/키       ", format(keyCount), format(membersPerKey));
        log.info("╚══════════════════════════════════════════════╝");

        long seedStart = System.currentTimeMillis();
        seedInteractionData(keyCount, membersPerKey);
        long seedElapsed = System.currentTimeMillis() - seedStart;

        long totalKeys = countRedisKeys("interaction:*");
        log.info("▶ 시드 완료: {}키, {}ms", format(totalKeys), format(seedElapsed));

        // 감쇠 실행 (3회 측정)
        List<Long> times = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            long start = System.currentTimeMillis();
            redisInteractionScoreRepository.applyInteractionScoreDecay();
            long elapsed = System.currentTimeMillis() - start;
            times.add(elapsed);
            log.info("  {}회차 감쇠: {}ms", i, format(elapsed));
        }

        long avg = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
        long remainingKeys = countRedisKeys("interaction:*");

        log.info("──────────────────────────────────────────────");
        log.info("▶ 평균: {}ms | 최소: {}ms | 최대: {}ms", format(avg), format(min), format(max));
        log.info("▶ 감쇠 후 남은 키: {}", format(remainingKeys));
        log.info("══════════════════════════════════════════════");
    }

    /**
     * 테스트용 interaction 시드 데이터를 파이프라인으로 생성합니다.
     */
    private void seedInteractionData(int keyCount, int membersPerKey) {
        int batchSize = 500;
        for (int i = 0; i < keyCount; i += batchSize) {
            int end = Math.min(i + batchSize, keyCount);
            int batchStart = i;

            stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (int k = batchStart; k < end; k++) {
                    byte[] key = createInteractionKey((long) k).getBytes(StandardCharsets.UTF_8);
                    for (int m = 0; m < membersPerKey; m++) {
                        long targetId = (long) (keyCount + k * membersPerKey + m);
                        double score = 1.0 + (m % 10) * 0.5; // 1.0 ~ 5.5 다양한 점수
                        connection.zSetCommands().zAdd(
                                key,
                                score,
                                String.valueOf(targetId).getBytes(StandardCharsets.UTF_8)
                        );
                    }
                }
                return null;
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Redis 헬퍼
    // ─────────────────────────────────────────────────────────────

    private void flushInteractionKeys() {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match("interaction:*").count(500).build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            // 1000개씩 삭제
            for (int i = 0; i < keys.size(); i += 1000) {
                stringRedisTemplate.delete(keys.subList(i, Math.min(i + 1000, keys.size())));
            }
            log.info("  Redis 삭제: interaction:* {}키", format(keys.size()));
        }
    }

    private long countRedisKeys(String pattern) {
        long count = 0;
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(500).build())) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
