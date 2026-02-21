package jaeik.bimillog.performance;

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
import org.springframework.data.redis.core.ZSetOperations;
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
    private static final int WARMUP_INTERVAL = 10;    // 1분 스케줄러 시뮬레이션 주기
    private static final int CAFFEINE_WARM_UP_SIZE = 100;
    private static final int TOP_N = 5;
    private static final double VIEW_SCORE = 2.0;
    private static final double MAX_ACCEPTABLE_ERROR_RATE = 0.90;
    private static final String SCORE_KEY = REALTIME_POST_SCORE_KEY;

    // [DEBUGGING] 디버깅용 누적 카운터
    private long totalEventsInClosed = 0;  // CLOSED 구간 누적 이벤트 수
    private long totalEventsInOpen = 0;    // OPEN 구간 누적 이벤트 수
    private int openSegmentCount = 0;      // OPEN 구간 시작 횟수

    // Zipf's Law 지수 (s 값)
    // 1.0에 가까울수록 표준적인 지프 분포, 클수록 상위 쏠림 심화
    private static final double ZIPF_EXPONENT = 1.2;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
        fallbackStore.clear();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("realtimeRedis");
        circuitBreaker.transitionToClosedState();
        // [DEBUGGING] 카운터 초기화
        totalEventsInClosed = 0;
        totalEventsInOpen = 0;
        openSegmentCount = 0;
    }

    @Test
    @DisplayName("서킷 토글 시 Redis Top5 vs Caffeine Top5 자카드 유사도 검증 (Zipf 분포 적용)")
    void shouldMaintainAcceptableRankDivergence_WhenCircuitToggles() {
        // Given: Zipf's Law를 적용한 가중치 분포 생성
        double[] weights = buildZipfSkewedWeights(POST_COUNT, ZIPF_EXPONENT);
        boolean circuitOpen = false;
        List<Double> similarities = new ArrayList<>();

        // [DEBUGGING] Zipf 분포 상위 10개 가중치 출력 (쏠림 정도 확인)
        log.info("[DEBUGGING] === 시뮬레이션 시작 (POST_COUNT={}, ZIPF_EXPONENT={}) ===", POST_COUNT, ZIPF_EXPONENT);
        double top1Weight = (POST_COUNT > 0) ? (weights[0]) : 0;
        double top5Weight = (POST_COUNT >= 5) ? (weights[4]) : weights[POST_COUNT - 1];
        double top10Weight = (POST_COUNT >= 10) ? (weights[9]) : weights[POST_COUNT - 1];
        log.info("[DEBUGGING] Zipf CDF: Top1={}, Top5={}, Top10={} (누적 선택 확률)",
                String.format("%.4f", top1Weight),
                String.format("%.4f", top5Weight),
                String.format("%.4f", top10Weight));
        log.info("[DEBUGGING] Top1 단독 선택 확률={}%, Top2~5 합산={}%",
                String.format("%.2f", top1Weight * 100),
                String.format("%.2f", (top5Weight - top1Weight) * 100));

        // When: 200라운드 시뮬레이션
        for (int round = 1; round <= TOTAL_ROUNDS; round++) {

            // 이벤트 발생: 5~10개 조회 이벤트 (항상 어댑터를 통해 호출)
            int eventCount = ThreadLocalRandom.current().nextInt(5, 11);

            // [DEBUGGING] 구간별 이벤트 수 누적
            if (circuitOpen) {
                totalEventsInOpen += eventCount;
            } else {
                totalEventsInClosed += eventCount;
            }

            for (int e = 0; e < eventCount; e++) {
                long postId = pickWeightedPostId(weights);
                // 실제 동작과 동일: 어댑터 호출 → 서킷 OPEN이면 fallback이 Caffeine에 적립
                redisPostRealTimeAdapter.incrementRealtimePopularScore(postId, VIEW_SCORE);
            }

            // 1분 스케줄러 시뮬레이션: CLOSED 구간에서 10라운드마다 Redis Top100 → Caffeine 웜업
            if (!circuitOpen && round % WARMUP_INTERVAL == 0) {
                Map<Long, Double> topScores = redisPostRealTimeAdapter.getTopNWithScores(CAFFEINE_WARM_UP_SIZE);
                if (!topScores.isEmpty()) {
                    fallbackStore.warmUp(topScores);
                }
                log.info("[DEBUGGING] 라운드 {} [웜업] Redis Top{} → Caffeine (Caffeine 항목수={})",
                        round, CAFFEINE_WARM_UP_SIZE, fallbackStore.size());
            }

            // 감쇠 적용 (50라운드마다)
            if (round % DECAY_INTERVAL == 0) {
                if (!circuitOpen) {
                    redisPostRealTimeAdapter.applyRealtimePopularScoreDecay();
                }
                fallbackStore.applyDecay();
                // [DEBUGGING] 감쇠 후 상태 출력
                Long redisSize = redisTemplate.opsForZSet().size(SCORE_KEY);
                log.info("[DEBUGGING] 라운드 {} 감쇠 적용 후: Redis 항목수={}, Caffeine 항목수={}, 현재 서킷={}",
                        round, redisSize, fallbackStore.size(), circuitOpen ? "OPEN" : "CLOSED");
            }

            // 서킷 토글 (20라운드마다)
            if (round % TOGGLE_INTERVAL == 0) {
                boolean wasOpen = circuitOpen;
                circuitOpen = !circuitOpen;

                // [DEBUGGING] 전환 직전 상태 스냅샷
                if (!wasOpen && circuitOpen) {
                    // CLOSED → OPEN 전환: Redis 히스토리 vs 방금 시작할 Caffeine
                    openSegmentCount++;
                    List<Long> redisTopAtToggle = getRedisTop(TOP_N);
                    Map<Long, Double> redisScoresAtToggle = getRedisTopWithScores(TOP_N);
                    Long redisTotalEntries = redisTemplate.opsForZSet().size(SCORE_KEY);
                    log.info("[DEBUGGING] === 라운드 {} CLOSED→OPEN 전환 ({}번째 OPEN 구간) ===",
                            round, openSegmentCount);
                    log.info("[DEBUGGING]   Redis 누적 이벤트(CLOSED 구간): {}, 현재 Redis 항목수: {}",
                            totalEventsInClosed, redisTotalEntries);
                    log.info("[DEBUGGING]   Redis Top{} 점수: {}", TOP_N, formatScoreMap(redisTopAtToggle, redisScoresAtToggle));
                    log.info("[DEBUGGING]   Caffeine 웜업 상태: 항목수={} (마지막 웜업 기준, 콜드스타트 없음)",
                            fallbackStore.size());
                } else if (wasOpen) {
                    // OPEN → CLOSED 전환
                    log.info("[DEBUGGING] === 라운드 {} OPEN→CLOSED 전환 ===", round);
                    log.info("[DEBUGGING]   OPEN 구간 누적 이벤트: {}, Caffeine 항목수: {}",
                            totalEventsInOpen, fallbackStore.size());
                    log.info("[DEBUGGING]   CLOSED 전환: 삭제 로그만 초기화, Caffeine 점수 유지 (다음 웜업까지 최대 {}라운드)", WARMUP_INTERVAL);
                }

                if (circuitOpen) {
                    circuitBreaker.transitionToOpenState();
                } else {
                    circuitBreaker.transitionToClosedState();
                }
            }

            // 서킷 OPEN 구간의 중간 지점에서만 비교 (30, 70, 110, ...)
            if (round % TOGGLE_INTERVAL == COMPARE_OFFSET && circuitOpen) {

                List<Long> redisTop = getRedisTop(TOP_N);
                List<Long> caffeineTop = fallbackStore.getTopPostIds(0, TOP_N);

                double jaccard = jaccardSimilarity(redisTop, caffeineTop);
                similarities.add(jaccard);

                // [DEBUGGING] 비교 시점 상세 분석
                Map<Long, Double> redisScores = getRedisTopWithScores(TOP_N);
                Map<Long, Double> caffeineScores = getCaffeineTopWithScores(TOP_N);
                Long redisTotalEntries = redisTemplate.opsForZSet().size(SCORE_KEY);

                Set<Long> intersection = new HashSet<>(redisTop);
                intersection.retainAll(new HashSet<>(caffeineTop));
                Set<Long> onlyInRedis = new HashSet<>(redisTop);
                onlyInRedis.removeAll(new HashSet<>(caffeineTop));
                Set<Long> onlyInCaffeine = new HashSet<>(caffeineTop);
                onlyInCaffeine.removeAll(new HashSet<>(redisTop));

                log.info("[DEBUGGING] --- 라운드 {} 비교 분석 ({}번째 OPEN 구간, OPEN 진입 후 {}라운드 경과) ---",
                        round, openSegmentCount, COMPARE_OFFSET);
                log.info("[DEBUGGING]   Redis 항목수={}, Caffeine 항목수={}",
                        redisTotalEntries, fallbackStore.size());
                log.info("[DEBUGGING]   CLOSED 구간 총 이벤트={}, OPEN 구간 총 이벤트={}",
                        totalEventsInClosed, totalEventsInOpen);
                log.info("[DEBUGGING]   누적 비율: Redis가 Caffeine보다 {}배 더 많은 이벤트 히스토리",
                        String.format("%.1f", totalEventsInClosed > 0 ? (double) totalEventsInClosed / Math.max(totalEventsInOpen, 1) : 0.0));
                log.info("[DEBUGGING]   Redis Top{} 점수: {}", TOP_N, formatScoreMap(redisTop, redisScores));
                log.info("[DEBUGGING]   Caffeine Top{} 점수: {}", TOP_N, formatScoreMap(caffeineTop, caffeineScores));
                log.info("[DEBUGGING]   교집합={}, Redis전용={}, Caffeine전용={}",
                        intersection, onlyInRedis, onlyInCaffeine);
                double top1Score = redisScores.values().stream().mapToDouble(d -> d).max().orElse(0);
                double top5Score = redisScores.values().stream().mapToDouble(d -> d).min().orElse(0);
                double scoreGap = top5Score > 0.001 ? top1Score / top5Score : 0;
                log.info("[DEBUGGING]   Redis Top1 점수={} vs Redis Top5 점수={} (점수 격차: {}배)",
                        String.format("%.2f", top1Score),
                        String.format("%.2f", top5Score),
                        String.format("%.2f", scoreGap));

                log.info("  라운드 {} [OPEN]: Redis={}, Caffeine={}, 유사도={}",
                        String.format("%3d", round), redisTop, caffeineTop, String.format("%.4f", jaccard));
            }
        }

        // Then: 평균 오차율 검증
        double avgSimilarity = similarities.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double avgErrorRate = 1.0 - avgSimilarity;

        // [DEBUGGING] 최종 종합 분석
        log.info("[DEBUGGING] ============================================================");
        log.info("[DEBUGGING] ===          최종 종합 분석 (POST_COUNT={})            ===", POST_COUNT);
        log.info("[DEBUGGING] ============================================================");
        log.info("[DEBUGGING] Zipf 지수: {}", ZIPF_EXPONENT);
        log.info("[DEBUGGING] Top1 단독 누적 선택 확률: {}%  (Zipf 쏠림 강도)", String.format("%.2f", weights[0] * 100));
        log.info("[DEBUGGING] Top5 누적 선택 확률: {}%", String.format("%.2f", (POST_COUNT >= 5 ? weights[4] : weights[POST_COUNT - 1]) * 100));
        log.info("[DEBUGGING] CLOSED 구간 총 이벤트: {} (Redis 히스토리)", totalEventsInClosed);
        log.info("[DEBUGGING] OPEN   구간 총 이벤트: {} (Caffeine 히스토리)", totalEventsInOpen);
        log.info("[DEBUGGING] 비교 시점 Redis:Caffeine 이벤트 비율 ≈ {}:{} (히스토리 비대칭)",
                totalEventsInClosed, totalEventsInOpen);
        log.info("[DEBUGGING] --- 비교 결과 ---");
        log.info("[DEBUGGING] 비교 횟수: {}", similarities.size());
        log.info("[DEBUGGING] 유사도 목록: {}", similarities.stream()
                .map(s -> String.format("%.4f", s)).toList());
        log.info("[DEBUGGING] 평균 자카드 유사도: {}", String.format("%.4f", avgSimilarity));
        log.info("[DEBUGGING] 평균 오차율:       {} ({}%)", String.format("%.4f", avgErrorRate), String.format("%.1f", avgErrorRate * 100));
        log.info("[DEBUGGING] --- 낮은 유사도의 원인 추정 ---");
        log.info("[DEBUGGING] POST_COUNT={}일 때 Top5 Zipf 누적 확률={}%",
                POST_COUNT, String.format("%.2f", (POST_COUNT >= 5 ? weights[4] : weights[POST_COUNT - 1]) * 100));
        log.info("[DEBUGGING] 항목이 많을수록 Top5 확률이 낮아지고, Redis(장기누적)와 Caffeine(단기누적)의 Top5가 달라짐");
        log.info("[DEBUGGING] ============================================================");

        log.info("[결과] 전환 횟수: {}, 평균 자카드 유사도: {}, 평균 오차율: {} (Zipf s={})",
                similarities.size(), String.format("%.4f", avgSimilarity),
                String.format("%.4f", avgErrorRate), ZIPF_EXPONENT);

        assertThat(avgErrorRate)
                .as("평균 오차율(1 - 자카드 유사도)이 90%% 이하여야 합니다. 실제: %.2f%%", avgErrorRate * 100)
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
     * [DEBUGGING] Redis ZSet에서 Top N의 postId → score 맵 조회
     */
    private Map<Long, Double> getRedisTopWithScores(int n) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(SCORE_KEY, 0, n - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<Object> t : tuples) {
            if (t.getValue() != null && t.getScore() != null) {
                result.put(((Number) t.getValue()).longValue(), t.getScore());
            }
        }
        return result;
    }

    /**
     * [DEBUGGING] Caffeine FallbackStore에서 Top N의 postId → score 맵 조회
     */
    private Map<Long, Double> getCaffeineTopWithScores(int n) {
        // FallbackStore 내부 캐시에 직접 접근하여 점수를 얻음
        // getTopPostIds는 id만 반환하므로, 직접 점수를 추출하기 위해 별도 구현
        List<Long> ids = fallbackStore.getTopPostIds(0, n);
        // 점수 정보를 얻을 수 없으므로 순위 기반 더미 점수를 부여 (상대적 순서 파악용)
        Map<Long, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            result.put(ids.get(i), (double) (n - i)); // 1위 = n점, 2위 = n-1점, ...
        }
        return result;
    }

    /**
     * [DEBUGGING] postId 목록과 점수 맵을 "postId(score)" 형식 문자열로 포맷
     */
    private String formatScoreMap(List<Long> ids, Map<Long, Double> scores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            Double score = scores.get(id);
            sb.append(id).append("(").append(score != null ? String.format("%.1f", score) : "?").append(")");
            if (i < ids.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
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
