package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisInteractionScoreRepository ZSet 변환 검증 테스트</h2>
 * <p>Hash에서 ZSet으로 변환된 상호작용 점수 저장소의 동작을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("RedisInteractionScoreRepository ZSet 로컬 통합 테스트")
class RedisInteractionScoreRepositoryLocalIntegrationTest {

    @Autowired
    private RedisInteractionScoreRepository repository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long MEMBER_1 = 1001L;
    private static final Long MEMBER_2 = 1002L;
    private static final Long MEMBER_3 = 1003L;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @AfterAll
    void tearDown() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("addInteractionScore - ZSet에 양방향으로 점수 저장")
    void shouldAddInteractionScoreBidirectionally() {
        // Given
        String idempotencyKey = "POST_LIKE:100:" + MEMBER_2;

        // When
        boolean result = repository.addInteractionScore(MEMBER_1, MEMBER_2, idempotencyKey);

        // Then
        assertThat(result).isTrue();

        // ZSet에 양방향으로 저장되었는지 확인
        String key1 = INTERACTION_PREFIX + MEMBER_1;
        String key2 = INTERACTION_PREFIX + MEMBER_2;

        Double score1 = redisTemplate.opsForZSet().score(key1, MEMBER_2);
        Double score2 = redisTemplate.opsForZSet().score(key2, MEMBER_1);

        assertThat(score1).isNotNull().isEqualTo(INTERACTION_SCORE_DEFAULT);
        assertThat(score2).isNotNull().isEqualTo(INTERACTION_SCORE_DEFAULT);
    }

    @Test
    @DisplayName("addInteractionScore - 멱등성 보장 (중복 호출 시 점수 미증가)")
    void shouldBeIdempotent() {
        // Given
        String idempotencyKey = "POST_LIKE:100:" + MEMBER_2;

        // When - 동일한 이벤트 2번 호출
        boolean first = repository.addInteractionScore(MEMBER_1, MEMBER_2, idempotencyKey);
        boolean second = repository.addInteractionScore(MEMBER_1, MEMBER_2, idempotencyKey);

        // Then
        assertThat(first).isTrue();
        assertThat(second).isFalse(); // 두 번째는 처리되지 않음

        // 점수는 한 번만 증가해야 함
        String key1 = INTERACTION_PREFIX + MEMBER_1;
        Double score = redisTemplate.opsForZSet().score(key1, MEMBER_2);
        assertThat(score).isEqualTo(INTERACTION_SCORE_DEFAULT);
    }

    @Test
    @DisplayName("addInteractionScore - 서로 다른 이벤트는 점수 누적")
    void shouldAccumulateScoreForDifferentEvents() {
        // Given
        String idempotencyKey1 = "POST_LIKE:100:" + MEMBER_2;
        String idempotencyKey2 = "COMMENT:200:" + MEMBER_2;

        // When
        repository.addInteractionScore(MEMBER_1, MEMBER_2, idempotencyKey1);
        repository.addInteractionScore(MEMBER_1, MEMBER_2, idempotencyKey2);

        // Then
        String key1 = INTERACTION_PREFIX + MEMBER_1;
        Double score = redisTemplate.opsForZSet().score(key1, MEMBER_2);
        assertThat(score).isEqualTo(INTERACTION_SCORE_DEFAULT * 2);
    }

    @Test
    @DisplayName("getInteractionScoresBatch - ZSet에서 배치 조회")
    void shouldGetScoresBatchFromZSet() {
        // Given - ZSet에 직접 데이터 시딩
        String key = INTERACTION_PREFIX + MEMBER_1;
        redisTemplate.opsForZSet().add(key, MEMBER_2, 2.5);
        redisTemplate.opsForZSet().add(key, MEMBER_3, 1.0);

        // When
        List<Object> results = repository.getInteractionScoresBatch(MEMBER_1, List.of(MEMBER_2, MEMBER_3, 9999L));

        // Then
        assertThat(results).hasSize(3);
        assertThat(Double.parseDouble(results.get(0).toString())).isEqualTo(2.5);
        assertThat(Double.parseDouble(results.get(1).toString())).isEqualTo(1.0);
        assertThat(results.get(2)).isNull(); // 존재하지 않는 멤버
    }

    @Test
    @DisplayName("getTopInteractionScores - ZSet 상위 N개 조회 (점수 내림차순)")
    void shouldGetTopInteractionScoresFromZSet() {
        // Given - ZSet에 직접 데이터 시딩
        String key = INTERACTION_PREFIX + MEMBER_1;
        redisTemplate.opsForZSet().add(key, MEMBER_2, 3.0);
        redisTemplate.opsForZSet().add(key, MEMBER_3, 1.5);

        // When
        Set<ZSetOperations.TypedTuple<Object>> results = repository.getTopInteractionScores(MEMBER_1, 10);

        // Then
        assertThat(results).hasSize(2);

        // 점수 내림차순 확인 (3.0 > 1.5)
        var resultList = results.stream().toList();
        assertThat(resultList.get(0).getScore()).isEqualTo(3.0);
        assertThat(resultList.get(1).getScore()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("getTopInteractionScores - 빈 ZSet 조회 시 빈 Set 반환")
    void shouldReturnEmptySetWhenNoInteractions() {
        // When
        Set<ZSetOperations.TypedTuple<Object>> results = repository.getTopInteractionScores(9999L, 10);

        // Then
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("getTopInteractionScores - limit보다 데이터가 많으면 상위 limit개만 반환")
    void shouldReturnOnlyLimitedResults() {
        // Given - 5개 데이터 시딩
        String key = INTERACTION_PREFIX + MEMBER_1;
        redisTemplate.opsForZSet().add(key, 100L, 5.0);
        redisTemplate.opsForZSet().add(key, 101L, 4.0);
        redisTemplate.opsForZSet().add(key, 102L, 3.0);
        redisTemplate.opsForZSet().add(key, 103L, 2.0);
        redisTemplate.opsForZSet().add(key, 104L, 1.0);

        // When - 상위 3개만 조회
        Set<ZSetOperations.TypedTuple<Object>> results = repository.getTopInteractionScores(MEMBER_1, 3);

        // Then
        assertThat(results).hasSize(3);

        var resultList = results.stream().toList();
        assertThat(resultList.get(0).getScore()).isEqualTo(5.0);
        assertThat(resultList.get(1).getScore()).isEqualTo(4.0);
        assertThat(resultList.get(2).getScore()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("applyInteractionScoreDecay - 지수 감쇠 적용")
    void shouldApplyDecayToAllScores() {
        // Given - 여러 회원의 상호작용 데이터 시딩
        String key1 = INTERACTION_PREFIX + MEMBER_1;
        String key2 = INTERACTION_PREFIX + MEMBER_2;

        redisTemplate.opsForZSet().add(key1, MEMBER_2, 5.0);
        redisTemplate.opsForZSet().add(key1, MEMBER_3, 2.0);
        redisTemplate.opsForZSet().add(key2, MEMBER_1, 3.0);

        // When
        int processedKeys = repository.applyInteractionScoreDecay();

        // Then
        assertThat(processedKeys).isEqualTo(2); // 2개 키 처리

        // 점수가 0.95 곱해졌는지 확인
        Double score1 = redisTemplate.opsForZSet().score(key1, MEMBER_2);
        Double score2 = redisTemplate.opsForZSet().score(key1, MEMBER_3);
        Double score3 = redisTemplate.opsForZSet().score(key2, MEMBER_1);

        assertThat(score1).isCloseTo(5.0 * INTERACTION_SCORE_DECAY_RATE, org.assertj.core.api.Assertions.within(0.001));
        assertThat(score2).isCloseTo(2.0 * INTERACTION_SCORE_DECAY_RATE, org.assertj.core.api.Assertions.within(0.001));
        assertThat(score3).isCloseTo(3.0 * INTERACTION_SCORE_DECAY_RATE, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    @DisplayName("applyInteractionScoreDecay - 임계값 이하 점수 삭제")
    void shouldRemoveScoresBelowThreshold() {
        // Given - 임계값(0.1) 이하의 낮은 점수 시딩
        String key = INTERACTION_PREFIX + MEMBER_1;
        redisTemplate.opsForZSet().add(key, MEMBER_2, 0.05); // 임계값 이하
        redisTemplate.opsForZSet().add(key, MEMBER_3, 1.0);  // 임계값 이상

        // When
        repository.applyInteractionScoreDecay();

        // Then
        Double removedScore = redisTemplate.opsForZSet().score(key, MEMBER_2);
        Double keptScore = redisTemplate.opsForZSet().score(key, MEMBER_3);

        assertThat(removedScore).isNull(); // 삭제됨
        assertThat(keptScore).isNotNull(); // 유지됨
    }

    @Test
    @DisplayName("deleteInteractionKeyByWithdraw - 탈퇴 회원 데이터 삭제")
    void shouldDeleteWithdrawnMemberData() {
        // Given
        String key1 = INTERACTION_PREFIX + MEMBER_1;
        String key2 = INTERACTION_PREFIX + MEMBER_2;
        String withdrawKey = INTERACTION_PREFIX + MEMBER_3;

        // MEMBER_3이 다른 회원들과 상호작용한 데이터 시딩
        redisTemplate.opsForZSet().add(key1, MEMBER_3, 2.0);
        redisTemplate.opsForZSet().add(key2, MEMBER_3, 1.5);
        redisTemplate.opsForZSet().add(withdrawKey, MEMBER_1, 3.0);
        redisTemplate.opsForZSet().add(withdrawKey, MEMBER_2, 2.5);

        // When - MEMBER_3 탈퇴
        repository.deleteInteractionKeyByWithdraw(MEMBER_3);

        // Then
        // 1. 탈퇴 회원의 키 삭제 확인
        assertThat(redisTemplate.hasKey(withdrawKey)).isFalse();

        // 2. 다른 회원의 ZSet에서 탈퇴 회원 제거 확인
        Double score1 = redisTemplate.opsForZSet().score(key1, MEMBER_3);
        Double score2 = redisTemplate.opsForZSet().score(key2, MEMBER_3);

        assertThat(score1).isNull();
        assertThat(score2).isNull();
    }
}
