package jaeik.bimillog.springboot.mysql.performance;

import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static jaeik.bimillog.infrastructure.redis.RedisKey.REALTIME_POST_SCORE_KEY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>Caffeine → Redis 동기화 성능 테스트</h2>
 * <p>서킷 OPEN → CLOSED 전환 시 Caffeine 누적 점수를 Redis에 동기화하는 시간을 측정합니다.</p>
 * <p>10,000건의 게시글 점수를 Caffeine에 적재한 뒤,
 * {@link RedisPostRealTimeAdapter#syncCaffeineScoresToRedis}와
 * {@link RedisPostRealTimeAdapter#replayDeletionsToRedis}의 소요 시간을 측정합니다.</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@DisplayName("Caffeine → Redis 동기화 성능 테스트 (10,000건)")
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@Tag("local-integration")
@Tag("performance")
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CaffeineFallbackStorePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(CaffeineFallbackStorePerformanceTest.class);

    @Autowired
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Autowired
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final int POST_COUNT = 10_000;
    private static final int DELETE_COUNT = 500;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
        realtimeScoreFallbackStore.clear();
    }

    @AfterEach
    void tearDown() {
        RedisTestHelper.flushRedis(redisTemplate);
        realtimeScoreFallbackStore.clear();
    }

    @Test
    @Order(1)
    @DisplayName("[성능] Caffeine 점수 → Redis ZINCRBY 파이프라인 동기화 (10,000건)")
    void measureSyncCaffeineScoresToRedis() {
        // Given: Caffeine에 10,000건 점수 적재 (서킷 OPEN 구간 시뮬레이션)
        for (long i = 1; i <= POST_COUNT; i++) {
            realtimeScoreFallbackStore.incrementScore(i, ThreadLocalRandom.current().nextDouble(2.0, 10.0));
        }
        assertThat(realtimeScoreFallbackStore.size()).isEqualTo(POST_COUNT);

        // When: CircuitBreakerEventConfig의 CLOSED 전환 로직과 동일
        Map<Long, Double> caffeineScores = realtimeScoreFallbackStore.getAllScores();

        long start = System.currentTimeMillis();
        if (!caffeineScores.isEmpty()) {
            redisPostRealTimeAdapter.syncCaffeineScoresToRedis(caffeineScores);
        }
        long elapsed = System.currentTimeMillis() - start;

        // Then
        Long redisSize = stringRedisTemplate.opsForZSet().zCard(REALTIME_POST_SCORE_KEY);
        assertThat(redisSize).isEqualTo(POST_COUNT);

        log.info("");
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  Caffeine → Redis 점수 동기화 (ZINCRBY 파이프라인)  ║");
        log.info("╠══════════════════════════════════════════════════╣");
        log.info("║  게시글 수  : {}건", String.format("%-30s║", String.format("%,d", POST_COUNT)));
        log.info("║  소요 시간  : {}ms", String.format("%-29s║", elapsed));
        log.info("║  Redis 키  : {}건", String.format("%-30s║", String.format("%,d", redisSize)));
        log.info("╚══════════════════════════════════════════════════╝");
    }

    @Test
    @Order(2)
    @DisplayName("[성능] 삭제 로그 → Redis ZREM 파이프라인 재처리 (500건)")
    void measureReplayDeletionsToRedis() {
        // Given: Redis에 10,000건 적재
        for (long i = 1; i <= POST_COUNT; i++) {
            realtimeScoreFallbackStore.incrementScore(i, ThreadLocalRandom.current().nextDouble(2.0, 10.0));
        }
        Map<Long, Double> caffeineScores = realtimeScoreFallbackStore.getAllScores();
        redisPostRealTimeAdapter.syncCaffeineScoresToRedis(caffeineScores);

        // 서킷 OPEN 구간에 500건 삭제 시뮬레이션
        for (long i = 1; i <= DELETE_COUNT; i++) {
            realtimeScoreFallbackStore.removePost(i);
        }
        Set<Long> deletedIds = realtimeScoreFallbackStore.getDeletedPostIds();
        assertThat(deletedIds).hasSize(DELETE_COUNT);

        // When: CircuitBreakerEventConfig의 CLOSED 전환 로직과 동일
        long start = System.currentTimeMillis();
        if (!deletedIds.isEmpty()) {
            redisPostRealTimeAdapter.replayDeletionsToRedis(deletedIds);
        }
        long elapsed = System.currentTimeMillis() - start;

        // Then
        Long redisSize = stringRedisTemplate.opsForZSet().zCard(REALTIME_POST_SCORE_KEY);
        assertThat(redisSize).isEqualTo(POST_COUNT - DELETE_COUNT);

        log.info("");
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  삭제 로그 → Redis 재처리 (ZREM 파이프라인)        ║");
        log.info("╠══════════════════════════════════════════════════╣");
        log.info("║  삭제 건수  : {}건", String.format("%-30s║", String.format("%,d", DELETE_COUNT)));
        log.info("║  소요 시간  : {}ms", String.format("%-29s║", elapsed));
        log.info("║  잔존 Redis : {}건", String.format("%-30s║", String.format("%,d", redisSize)));
        log.info("╚══════════════════════════════════════════════════╝");
    }

    @Test
    @Order(3)
    @DisplayName("[성능] 종합 — 점수 동기화 + 삭제 재처리 전체 소요 시간 (10,000건 + 500건 삭제)")
    void measureFullSyncOnCircuitClose() {
        // Given: Caffeine에 10,000건 적재 + 500건 삭제
        for (long i = 1; i <= POST_COUNT; i++) {
            realtimeScoreFallbackStore.incrementScore(i, ThreadLocalRandom.current().nextDouble(2.0, 10.0));
        }
        for (long i = 1; i <= DELETE_COUNT; i++) {
            realtimeScoreFallbackStore.removePost(i);
        }

        // When: CircuitBreakerEventConfig.onStateTransition CLOSED 전환 전체 로직
        long start = System.currentTimeMillis();

        Map<Long, Double> caffeineScores = realtimeScoreFallbackStore.getAllScores();
        if (!caffeineScores.isEmpty()) {
            redisPostRealTimeAdapter.syncCaffeineScoresToRedis(caffeineScores);
        }

        Set<Long> deletedIds = realtimeScoreFallbackStore.getDeletedPostIds();
        if (!deletedIds.isEmpty()) {
            redisPostRealTimeAdapter.replayDeletionsToRedis(deletedIds);
        }

        long elapsed = System.currentTimeMillis() - start;

        // Then
        Long redisSize = stringRedisTemplate.opsForZSet().zCard(REALTIME_POST_SCORE_KEY);

        log.info("");
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  종합: CLOSED 전환 전체 동기화                     ║");
        log.info("╠══════════════════════════════════════════════════╣");
        log.info("║  점수 동기화 : {}건", String.format("%-29s║", String.format("%,d", caffeineScores.size())));
        log.info("║  삭제 재처리 : {}건", String.format("%-29s║", String.format("%,d", deletedIds.size())));
        log.info("║  총 소요     : {}ms", String.format("%-28s║", elapsed));
        log.info("║  Redis 잔존  : {}건", String.format("%-29s║", String.format("%,d", redisSize)));
        log.info("╚══════════════════════════════════════════════════╝");
    }
}
