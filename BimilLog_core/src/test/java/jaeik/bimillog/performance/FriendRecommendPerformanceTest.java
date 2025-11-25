package jaeik.bimillog.performance;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.event.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.friend.service.FriendRecommendService;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import jaeik.bimillog.testutil.RedisTestHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.awaitility.Awaitility;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>친구 추천 API 성능 측정 테스트</h2>
 * <p>Hibernate Statistics를 사용하여 쿼리 성능을 측정합니다.</p>
 * <p>시드 데이터: 1,000명 회원, 평균 15명 친구</p>
 * <p>DB: bimillogTest (local-integration 프로파일)</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FriendRecommendPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(FriendRecommendPerformanceTest.class);

    @Autowired
    private FriendRecommendService friendRecommendService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private RedisFriendshipRepository redisFriendshipRepository;

    @Autowired
    private RedisInteractionScoreRepository redisInteractionScoreRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Statistics statistics;
    private static final int TEST_COUNT = 10;

    private static final Duration SEED_TIMEOUT = Duration.ofSeconds(10);

    @BeforeAll
    void beforeAll() {
        log.info("========================================");
        log.info("친구 추천 API 성능 측정 테스트 (10회 조회)");
        log.info("========================================");

        seedRedisCaches();
    }

    @BeforeEach
    void setUp() {
        // Hibernate Statistics 초기화
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("친구 추천 API 성능 측정 (10회 조회)")
    void measureRecommendFriendPerformance() {
        log.info("========================================");
        log.info("친구 추천 조회 성능 측정 ({}회)", TEST_COUNT);
        log.info("========================================");

        Pageable pageable = PageRequest.of(0, 10);
        List<Long> responseTimes = new ArrayList<>();

        // Statistics 초기화
        statistics.clear();

        StopWatch totalStopWatch = new StopWatch();
        totalStopWatch.start();

        // 10회 조회
        for (int i = 0; i < TEST_COUNT; i++) {
            Long memberId = (long) (i + 1);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            friendRecommendService.getRecommendFriendList(memberId, pageable);

            stopWatch.stop();
            responseTimes.add(stopWatch.getTotalTimeMillis());
        }

        totalStopWatch.stop();

        // 통계 계산
        long totalTime = totalStopWatch.getTotalTimeMillis();
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        // 결과 출력
        log.info("\n========== 성능 측정 결과 ({} 요청) ==========", TEST_COUNT);
        log.info("총 소요 시간: {}ms", totalTime);
        log.info("평균 응답 시간: {}ms", avgTime);
        log.info("최소 응답 시간: {}ms", minTime);
        log.info("최대 응답 시간: {}ms", maxTime);
        log.info("");

        printHibernateStatistics();

        // 성능 기준 검증
        Assertions.assertTrue(avgTime < 1000, "평균 응답 시간이 1초를 초과했습니다.");
    }

    /**
     * Hibernate Statistics 출력
     */
    private void printHibernateStatistics() {
        log.info("========== Hibernate Statistics ==========");
        log.info("쿼리 실행 횟수: {}", statistics.getQueryExecutionCount());
        log.info("쿼리 실행 총 시간: {}ms", statistics.getQueryExecutionMaxTime());

        String slowestQuery = statistics.getQueryExecutionMaxTimeQueryString();
        if (slowestQuery != null && !slowestQuery.isEmpty()) {
            log.info("가장 느린 쿼리: {}", slowestQuery.length() > 100 ?
                slowestQuery.substring(0, 100) + "..." : slowestQuery);
        }
        log.info("");

        log.info("엔티티 로드 횟수: {}", statistics.getEntityLoadCount());
        log.info("엔티티 삽입 횟수: {}", statistics.getEntityInsertCount());
        log.info("엔티티 업데이트 횟수: {}", statistics.getEntityUpdateCount());
        log.info("엔티티 삭제 횟수: {}", statistics.getEntityDeleteCount());
        log.info("");

        log.info("2차 캐시 조회 횟수: {}", statistics.getSecondLevelCacheHitCount());
        log.info("2차 캐시 미스 횟수: {}", statistics.getSecondLevelCacheMissCount());
        log.info("");

        log.info("세션 열기 횟수: {}", statistics.getSessionOpenCount());
        log.info("트랜잭션 횟수: {}", statistics.getTransactionCount());
        log.info("플러시 횟수: {}", statistics.getFlushCount());
        log.info("==========================================");
    }

    /**
     * 로컬 통합 환경 Redis 캐시 시딩
     * <p>테스트 시작 전 실제 서비스와 동일하게 이벤트를 발행해 친구 관계 및 상호작용 점수를 Redis(6380) 에 적재합니다.</p>
     */
    private void seedRedisCaches() {
        RedisTestHelper.flushRedis(redisTemplate);

        List<Friendship> friendships = friendshipRepository.findAll();
        if (friendships.isEmpty()) {
            log.warn("시드할 친구 관계가 없습니다. DB 데이터를 확인하세요.");
            return;
        }

        seedFriendships(friendships);
        seedInteractionScores(friendships);
    }

    private void seedFriendships(List<Friendship> friendships) {
        friendships.forEach(friendship -> eventPublisher.publishEvent(
                new FriendshipCreatedEvent(friendship.getMember().getId(), friendship.getFriend().getId())
        ));

        Long sampleMemberId = friendships.get(0).getMember().getId();
        Awaitility.await()
                .atMost(SEED_TIMEOUT)
                .untilAsserted(() -> assertThat(redisFriendshipRepository.getFriends(sampleMemberId)).isNotEmpty());
    }

    private void seedInteractionScores(List<Friendship> friendships) {
        AtomicLong postIdSequence = new AtomicLong(1L);
        friendships.forEach(friendship -> eventPublisher.publishEvent(
                new PostLikeEvent(postIdSequence.getAndIncrement(), friendship.getMember().getId(), friendship.getFriend().getId())
        ));

        Long sampleMemberId = friendships.get(0).getMember().getId();
        Set<Long> sampleTargets = redisFriendshipRepository.getFriends(sampleMemberId);

        if (!sampleTargets.isEmpty()) {
            Awaitility.await()
                    .atMost(SEED_TIMEOUT)
                    .untilAsserted(() -> assertThat(redisInteractionScoreRepository
                            .getInteractionScoresBatch(sampleMemberId, sampleTargets)).isNotEmpty());
        }
    }
}
