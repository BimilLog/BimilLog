//package jaeik.bimillog.springboot.mysql.performance;
//
//import io.github.resilience4j.circuitbreaker.CircuitBreaker;
//import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
//import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
//import jaeik.bimillog.domain.post.entity.jpa.Post;
//import jaeik.bimillog.domain.post.repository.PostQueryRepository;
//import jaeik.bimillog.domain.post.repository.PostQueryType;
//import jaeik.bimillog.domain.post.repository.PostRepository;
//import jaeik.bimillog.domain.member.entity.Member;
//import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
//import jaeik.bimillog.testutil.RedisTestHelper;
//import jaeik.bimillog.testutil.TestMembers;
//import jaeik.bimillog.testutil.fixtures.TestFixtures;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.concurrent.ThreadLocalRandom;
//
//import static jaeik.bimillog.infrastructure.redis.RedisKey.REALTIME_POST_SCORE_KEY;
//
//
///**
// * <h2>실시간 인기글 Redis vs DB 폴백 순위 유사도 검증 테스트</h2>
// * <p>Redis ZSet 기반 실시간 인기글과 DB 폴백({@code findRecentPopularPosts}) 결과의
// * 순위 유사도를 측정합니다.</p>
// *
// * <p>DB 게시글은 무작위 초기 조회수/추천수로 생성되고,
// * Redis 점수는 Zipf's Law 분포로 조회 이벤트를 발생시켜 증가합니다.
// * 가중치 체계가 다른 두 시스템 간의 순위 차이가 허용 범위 내인지 검증합니다.</p>
// *
// * @author Jaeik
// * @version 1.0.0
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@Tag("local-integration")
//@Tag("performance")
//@ActiveProfiles("local-integration")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@Transactional
//class RealtimeDbFallbackConsistencyTest {
//    private static final Logger log = LoggerFactory.getLogger(RealtimeDbFallbackConsistencyTest.class);
//
//    @Autowired
//    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;
//
//    @Autowired
//    private PostRepository postRepository;
//
//    @Autowired
//    private PostQueryRepository postQueryRepository;
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private CircuitBreakerRegistry circuitBreakerRegistry;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    private static final int POST_COUNT = 1000;
//    private static final int TOTAL_ROUNDS = 100;
//    private static final int COMPARE_INTERVAL = 10;
//    private static final int TOP_N = 5;
//    private static final double VIEW_SCORE = 2.0;
//    private static final int MAX_INITIAL_VIEWS = 50;
//    private static final String SCORE_KEY = REALTIME_POST_SCORE_KEY;
//    private static final double ZIPF_EXPONENT = 1.2;
//
//    private final List<Post> testPosts = new ArrayList<>();
//
//    @BeforeEach
//    void setUp() {
//        RedisTestHelper.flushRedis(redisTemplate);
//
//        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("realtimeRedis");
//        cb.transitionToClosedState();
//
//        Member testMember = TestMembers.createUniqueWithPrefix("dbfallback");
//        TestFixtures.persistMemberWithDependencies(entityManager, testMember);
//
//        // 글 생성: 무작위 초기 조회수 (1000개, 좋아요 없음)
//        for (int i = 1; i <= POST_COUNT; i++) {
//            int initialViews = ThreadLocalRandom.current().nextInt(0, MAX_INITIAL_VIEWS + 1);
//            Post post = Post.builder()
//                    .member(testMember)
//                    .title("테스트 게시글 " + i)
//                    .content("내용 " + i)
//                    .views(initialViews)
//                    .password(1234)
//                    .build();
//            testPosts.add(postRepository.save(post));
//        }
//        entityManager.flush();
//        entityManager.clear();
//    }
//
//    @Test
//    @DisplayName("Redis 실시간 인기글 Top5 vs DB 폴백 Top5 자카드 유사도 검증 (조회 이벤트)")
//    void shouldMaintainAcceptableRankDivergence_BetweenRedisAndDbFallback() {
//        // Given
//        double[] weights = buildZipfSkewedWeights(POST_COUNT, ZIPF_EXPONENT);
//        List<Integer> measuredRounds = new ArrayList<>();
//        List<Double> similarities = new ArrayList<>();
//
//        // When: 100라운드 시뮬레이션 (조회 이벤트만)
//        for (int round = 1; round <= TOTAL_ROUNDS; round++) {
//
//            // Redis 점수만 Zipf 분포로 증가 (DB는 초기값 그대로)
//            int viewEvents = ThreadLocalRandom.current().nextInt(5, 11);
//            for (int e = 0; e < viewEvents; e++) {
//                int postIdx = pickWeightedIndex(weights);
//                Post post = testPosts.get(postIdx);
//                redisPostRealTimeAdapter.incrementRealtimePopularScore(post.getId(), VIEW_SCORE);
//            }
//
//            if (round % COMPARE_INTERVAL == 0) {
//                entityManager.flush();
//                entityManager.clear();
//
//                List<Long> redisTop = getRedisTop(TOP_N);
//                Page<PostSimpleDetail> dbPage = postQueryRepository.selectPostSimpleDetails(
//                        PostQueryType.REALTIME_FALLBACK.condition(),
//                        PageRequest.of(0, TOP_N),
//                        PostQueryType.REALTIME_FALLBACK.getOrders());
//                List<Long> dbTop = dbPage.getContent().stream()
//                        .map(PostSimpleDetail::getId)
//                        .toList();
//
//                double jaccard = jaccardSimilarity(redisTop, dbTop);
//                measuredRounds.add(round);
//                similarities.add(jaccard);
//
//                log.info("  라운드 {}: Redis={}, DB={}, 유사도={}",
//                        String.format("%3d", round), redisTop, dbTop, String.format("%.4f", jaccard));
//            }
//        }
//
//        // Then
//        double avgSimilarity = similarities.stream()
//                .mapToDouble(Double::doubleValue)
//                .average()
//                .orElse(0.0);
//        double avgErrorRate = 1.0 - avgSimilarity;
//
//        log.info("============================================================");
//        log.info("[Redis vs DB 폴백] 측정 라운드: {}", measuredRounds);
//        log.info("[Redis vs DB 폴백] 유사도 목록: {}", similarities.stream()
//                .map(s -> String.format("%.4f", s)).toList());
//        log.info("[Redis vs DB 폴백] 평균 유사도: {}, 평균 오차율: {}%",
//                String.format("%.4f", avgSimilarity),
//                String.format("%.1f", avgErrorRate * 100));
//        log.info("============================================================");
//    }
//
//    // ========== 유틸리티 메서드 ==========
//
//    private double[] buildZipfSkewedWeights(int count, double exponent) {
//        double[] weights = new double[count];
//        double total = 0;
//        for (int i = 0; i < count; i++) {
//            weights[i] = 1.0 / Math.pow(i + 1, exponent);
//            total += weights[i];
//        }
//        double cumulative = 0;
//        for (int i = 0; i < count; i++) {
//            cumulative += weights[i] / total;
//            weights[i] = cumulative;
//        }
//        return weights;
//    }
//
//    private int pickWeightedIndex(double[] cumulativeWeights) {
//        double r = ThreadLocalRandom.current().nextDouble();
//        for (int i = 0; i < cumulativeWeights.length; i++) {
//            if (r <= cumulativeWeights[i]) {
//                return i;
//            }
//        }
//        return cumulativeWeights.length - 1;
//    }
//
//    private List<Long> getRedisTop(int n) {
//        Set<Object> set = redisTemplate.opsForZSet().reverseRange(SCORE_KEY, 0, n - 1);
//        if (set == null || set.isEmpty()) {
//            return List.of();
//        }
//        return set.stream()
//                .map(obj -> ((Number) obj).longValue())
//                .toList();
//    }
//
//    private double jaccardSimilarity(List<Long> a, List<Long> b) {
//        if (a.isEmpty() && b.isEmpty()) return 1.0;
//        if (a.isEmpty() || b.isEmpty()) return 0.0;
//        Set<Long> setA = new HashSet<>(a);
//        Set<Long> setB = new HashSet<>(b);
//        Set<Long> intersection = new HashSet<>(setA);
//        intersection.retainAll(setB);
//        Set<Long> union = new HashSet<>(setA);
//        union.addAll(setB);
//        return (double) intersection.size() / union.size();
//    }
//}
