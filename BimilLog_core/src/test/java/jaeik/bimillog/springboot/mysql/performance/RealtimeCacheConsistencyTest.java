package jaeik.bimillog.springboot.mysql.performance;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static jaeik.bimillog.infrastructure.redis.RedisKey.REALTIME_POST_SCORE_KEY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>실시간 인기글 서킷 개폐 시 Redis-Caffeine 순위 오차율 검증 테스트</h2>
 * <p>서킷이 열리고 닫힐 때 Redis ZSet과 Caffeine FallbackStore의 순위 결과가
 * SNS 특성상 큰 차이가 없음을 실제 Redis 환경에서 검증합니다.</p>
 *
 * <p>실제 동작과 동일하게 {@link RedisPostRealTimeAdapter#incrementRealtimePopularScore}를
 * 항상 호출하고, 서킷 개폐만 직접 제어합니다.
 * 서킷 OPEN 시 어댑터의 fallback이 자동으로 Caffeine에 점수를 적립합니다.</p>
 *
 * <p>트래픽 분포는 Zipf's Law(지프의 법칙) 수식을 적용하여
 * 실제 SNS의 '쏠림 현상'을 시뮬레이션합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
@Tag("performance")
class RealtimeCacheConsistencyTest {
    private static final Logger log = LoggerFactory.getLogger(RealtimeCacheConsistencyTest.class);

    @Autowired
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Autowired
    private RealtimeScoreFallbackStore fallbackStore;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    private static final int POST_COUNT = 1000;
    private static final int TOTAL_ROUNDS = 200;
    private static final int TOGGLE_INTERVAL = 20;
    private static final int COMPARE_OFFSET = TOGGLE_INTERVAL / 2; // 전환 사이 중간 지점에서 비교
    private static final int DECAY_INTERVAL = 50;
    private static final int WARMUP_INTERVAL = 10;
    private static final int CAFFEINE_WARM_UP_SIZE = 100;
    private static final int TOP_N = 5;
    private static final double VIEW_SCORE = 2.0;
    private static final double MAX_ACCEPTABLE_ERROR_RATE = 0.90;
    private static final String SCORE_KEY = REALTIME_POST_SCORE_KEY;

    // Zipf's Law 지수 (s 값)
    // 1.0에 가까울수록 표준적인 지프 분포, 클수록 상위 쏠림 심화
    private static final double ZIPF_EXPONENT = 1.2;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
        fallbackStore.clear();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("realtimeRedis");
        circuitBreaker.transitionToClosedState();
    }

    @Test
    @DisplayName("서킷 토글 시 Redis Top5 vs Caffeine Top5 자카드 유사도 검증 (Zipf 분포 적용)")
    void shouldMaintainAcceptableRankDivergence_WhenCircuitToggles() {
        // Given: Zipf's Law를 적용한 가중치 분포 생성
        double[] weights = buildZipfSkewedWeights(POST_COUNT, ZIPF_EXPONENT);
        boolean circuitOpen = false;

        // 직후: 서킷 OPEN 전환 시점 (해당 라운드 이벤트는 CLOSED 구간에서 발생)
        List<Integer> immediateRounds = new ArrayList<>();
        List<Double> immediateAfterOpenSimilarities = new ArrayList<>();

        // 중간: OPEN 진입 후 COMPARE_OFFSET 라운드 경과 시점
        List<Integer> midpointRounds = new ArrayList<>();
        List<Double> midpointSimilarities = new ArrayList<>();

        // When: 200라운드 시뮬레이션
        for (int round = 1; round <= TOTAL_ROUNDS; round++) {

            // 이벤트 발생: 5~10개 조회 이벤트 (항상 어댑터를 통해 호출)
            int eventCount = ThreadLocalRandom.current().nextInt(5, 11);
            for (int e = 0; e < eventCount; e++) {
                long postId = pickWeightedPostId(weights);
                redisPostRealTimeAdapter.incrementRealtimePopularScore(postId, VIEW_SCORE);
            }

            // 1분 스케줄러 시뮬레이션: CLOSED 구간에서 10라운드마다 Redis Top100 → Caffeine 웜업
            if (!circuitOpen && round % WARMUP_INTERVAL == 0) {
                Map<Long, Double> topScores = redisPostRealTimeAdapter.getTopNWithScores(CAFFEINE_WARM_UP_SIZE);
                if (!topScores.isEmpty()) {
                    fallbackStore.warmUp(topScores);
                }
            }

            // 감쇠 적용 (50라운드마다)
            if (round % DECAY_INTERVAL == 0) {
                if (!circuitOpen) {
                    redisPostRealTimeAdapter.applyRealtimePopularScoreDecay();
                }
                fallbackStore.applyDecay();
            }

            // 서킷 토글 (20라운드마다)
            if (round % TOGGLE_INTERVAL == 0) {
                circuitOpen = !circuitOpen;

                if (circuitOpen) {
                    circuitBreaker.transitionToOpenState();

                    // OPEN 직후 비교
                    List<Long> redisTop = getRedisTop(TOP_N);
                    List<Long> caffeineTop = fallbackStore.getTopPostIds(0, TOP_N);
                    double jaccard = jaccardSimilarity(redisTop, caffeineTop);
                    immediateRounds.add(round);
                    immediateAfterOpenSimilarities.add(jaccard);

                    log.info("  라운드 {} [OPEN 직후]: Redis={}, Caffeine={}, 유사도={}",
                            String.format("%3d", round), redisTop, caffeineTop, String.format("%.4f", jaccard));
                } else {
                    circuitBreaker.transitionToClosedState();
                }
            }

            // OPEN 구간 중간 지점 비교
            if (round % TOGGLE_INTERVAL == COMPARE_OFFSET && circuitOpen) {
                List<Long> redisTop = getRedisTop(TOP_N);
                List<Long> caffeineTop = fallbackStore.getTopPostIds(0, TOP_N);
                double jaccard = jaccardSimilarity(redisTop, caffeineTop);
                midpointRounds.add(round);
                midpointSimilarities.add(jaccard);

                log.info("  라운드 {} [OPEN 중간]: Redis={}, Caffeine={}, 유사도={}",
                        String.format("%3d", round), redisTop, caffeineTop, String.format("%.4f", jaccard));
            }
        }

        // Then: 최종 결과 집계
        double avgImmediateSimilarity = immediateAfterOpenSimilarities.stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgImmediateErrorRate = 1.0 - avgImmediateSimilarity;

        double avgMidpointSimilarity = midpointSimilarities.stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgMidpointErrorRate = 1.0 - avgMidpointSimilarity;

        log.info("============================================================");
        log.info("[OPEN 직후] 측정 라운드: {}", immediateRounds);
        log.info("[OPEN 직후] 유사도 목록: {}", immediateAfterOpenSimilarities.stream()
                .map(s -> String.format("%.4f", s)).toList());
        log.info("[OPEN 직후] 평균 유사도: {}, 평균 오차율: {}%",
                String.format("%.4f", avgImmediateSimilarity),
                String.format("%.1f", avgImmediateErrorRate * 100));
        log.info("------------------------------------------------------------");
        log.info("[OPEN 중간] 측정 라운드: {}", midpointRounds);
        log.info("[OPEN 중간] 유사도 목록: {}", midpointSimilarities.stream()
                .map(s -> String.format("%.4f", s)).toList());
        log.info("[OPEN 중간] 평균 유사도: {}, 평균 오차율: {}%",
                String.format("%.4f", avgMidpointSimilarity),
                String.format("%.1f", avgMidpointErrorRate * 100));
        log.info("============================================================");

        assertThat(avgMidpointErrorRate)
                .as("평균 오차율(1 - 자카드 유사도)이 90%% 이하여야 합니다. 실제: %.2f%%", avgMidpointErrorRate * 100)
                .isLessThanOrEqualTo(MAX_ACCEPTABLE_ERROR_RATE);
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * Zipf's Law (P = 1/r^s)를 적용한 가중치 분포 생성
     *
     * @param count    전체 아이템 수 (N)
     * @param exponent Zipf 지수 (s) - 보통 1.0 내외
     */
    private double[] buildZipfSkewedWeights(int count, double exponent) {
        double[] weights = new double[count];
        double total = 0;

        // 1. 각 순위(rank)별 가중치 계산: 1 / (rank ^ s)
        for (int i = 0; i < count; i++) {
            int rank = i + 1;
            weights[i] = 1.0 / Math.pow(rank, exponent);
            total += weights[i];
        }

        // 2. 누적 분포(CDF)로 변환 (0.0 ~ 1.0 사이 값으로 정규화)
        double cumulative = 0;
        for (int i = 0; i < count; i++) {
            cumulative += weights[i] / total;
            weights[i] = cumulative;
        }
        return weights;
    }

    /**
     * 가중치 분포(CDF)에 따라 postId 선택
     */
    private long pickWeightedPostId(double[] cumulativeWeights) {
        double r = ThreadLocalRandom.current().nextDouble();
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (r <= cumulativeWeights[i]) {
                return i + 1L;
            }
        }
        return cumulativeWeights.length;
    }

    /**
     * Redis ZSet에서 점수 내림차순 Top N 조회
     */
    private List<Long> getRedisTop(int n) {
        Set<Object> set = redisTemplate.opsForZSet().reverseRange(SCORE_KEY, 0, n - 1);
        if (set == null || set.isEmpty()) {
            return List.of();
        }
        return set.stream()
                .map(obj -> ((Number) obj).longValue())
                .toList();
    }

    /**
     * 자카드 유사도 계산: |교집합| / |합집합|
     */
    private double jaccardSimilarity(List<Long> a, List<Long> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<Long> setA = new HashSet<>(a);
        Set<Long> setB = new HashSet<>(b);
        Set<Long> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<Long> union = new HashSet<>(setA);
        union.addAll(setB);
        return (double) intersection.size() / union.size();
    }
}
