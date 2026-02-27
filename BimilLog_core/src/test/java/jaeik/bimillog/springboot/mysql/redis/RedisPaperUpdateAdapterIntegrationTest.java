package jaeik.bimillog.springboot.mysql.redis;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperUpdateAdapter;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPaperUpdateAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 롤링페이퍼 캐시 갱신 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("local-integration")
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RedisPaperUpdateAdapterIntegrationTest {

    @Autowired
    private RedisPaperUpdateAdapter redisPaperUpdateAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SCORE_KEY = RedisKey.REALTIME_PAPER_SCORE_KEY;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 롤링페이퍼 점수 증가")
    void shouldIncrementScore_WhenMemberIdAndScoreProvided() {
        // Given
        Long memberId = 1L;
        double score = 5.0; // 메시지 작성 점수

        // When: 점수 증가
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, score);

        // Then: Sorted Set에서 점수 확인
        Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, memberId.toString());
        assertThat(currentScore).isEqualTo(5.0);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 롤링페이퍼 점수 누적")
    void shouldAccumulateScore_WhenMultipleIncrementsOccur() {
        // Given
        Long memberId = 1L;

        // When: 여러 번 점수 증가 (조회 2점 + 메시지 5점 + 조회 2점)
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, 2.0); // 조회
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, 5.0); // 메시지 작성
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, 2.0); // 조회

        // Then: 누적 점수 확인
        Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, memberId.toString());
        assertThat(currentScore).isEqualTo(9.0); // 2 + 5 + 2
    }

    @Test
    @DisplayName("정상 케이스 - 음수 점수 증가 (메시지 삭제)")
    void shouldDecrementScore_WhenNegativeScoreProvided() {
        // Given: 초기 점수 10점
        Long memberId = 1L;
        redisTemplate.opsForZSet().add(SCORE_KEY, memberId.toString(), 10.0);

        // When: 메시지 삭제로 -5점
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, -5.0);

        // Then: 점수 감소 확인
        Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, memberId.toString());
        assertThat(currentScore).isEqualTo(5.0); // 10 - 5
    }

    @Test
    @DisplayName("정상 케이스 - 여러 롤링페이퍼 동시 점수 증가")
    void shouldIncrementScoresIndependently_WhenMultipleMembersUpdated() {
        // Given
        Long memberId1 = 1L;
        Long memberId2 = 2L;
        Long memberId3 = 3L;

        // When: 여러 롤링페이퍼에 점수 증가
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId1, 5.0);
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId2, 3.0);
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId3, 7.0);

        // Then: 각각 독립적으로 점수 증가
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, memberId1.toString())).isEqualTo(5.0);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, memberId2.toString())).isEqualTo(3.0);
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, memberId3.toString())).isEqualTo(7.0);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 롤링페이퍼 점수 감쇠 적용 (Lua 스크립트)")
    void shouldApplyDecay_WhenScoreDecayInvoked() {
        // Given: 여러 롤링페이퍼에 초기 점수 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 10.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 5.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "3", 2.0);

        // When: 감쇠 적용 (0.95배)
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();

        // Then: 점수가 0.95배로 감소
        Double score1 = redisTemplate.opsForZSet().score(SCORE_KEY, "1");
        Double score2 = redisTemplate.opsForZSet().score(SCORE_KEY, "2");
        Double score3 = redisTemplate.opsForZSet().score(SCORE_KEY, "3");

        assertThat(score1).isEqualTo(9.7);  // 10 * 0.97
        assertThat(score2).isEqualTo(4.85); // 5 * 0.97
        assertThat(score3).isEqualTo(1.94);  // 2 * 0.97
    }

    @ParameterizedTest
    @MethodSource("provideDecayScenarios")
    @DisplayName("정상 케이스 - 점수 감쇠 후 임계값 검증 (다양한 데이터 크기)")
    void shouldHandleDecay_VariousScenarios(Map<String, Double> initialScores, int expectedRemaining, Set<String> expectedRemainingIds) {
        // Given: 초기 점수 설정
        initialScores.forEach((id, score) ->
            redisTemplate.opsForZSet().add(SCORE_KEY, id, score)
        );

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(initialSize).isEqualTo(initialScores.size());

        // When: 감쇠 적용
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();

        // Then: 예상된 수만큼 남아있음
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(expectedRemaining);

        // 남아있는 항목 확인
        if (expectedRemaining > 0) {
            Set<Object> remainingPapers = redisTemplate.opsForZSet().range(SCORE_KEY, 0, -1);
            assertThat(remainingPapers).containsExactlyInAnyOrderElementsOf(expectedRemainingIds);
        }

        // 제거된 항목 확인
        initialScores.keySet().stream()
            .filter(id -> !expectedRemainingIds.contains(id))
            .forEach(removedId ->
                assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, removedId)).isNull()
            );
    }

    static Stream<Arguments> provideDecayScenarios() {
        return Stream.of(
            // 4개 점수, 2개 남음 (10.0, 1.5 유지)
            Arguments.of(
                Map.of("1", 10.0, "2", 1.5, "3", 1.03, "4", 0.8),
                2,
                Set.of("1", "2")
            ),
            // 3개 점수, 모두 제거
            Arguments.of(
                Map.of("1", 1.03, "2", 0.9, "3", 0.8),
                0,
                Set.of()
            ),
            // 1개 점수 (정확히 임계값), 제거됨
            Arguments.of(
                Map.of("1", 1.0),
                0,
                Set.of()
            )
        );
    }

    @Test
    @DisplayName("정상 케이스 - 빈 Sorted Set에 감쇠 적용")
    void shouldHandleEmptySet_WhenDecayApplied() {
        // Given: 빈 Sorted Set

        // When: 감쇠 적용
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();

        // Then: 에러 없이 정상 처리
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isZero();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 번 감쇠 적용")
    void shouldApplyDecayMultipleTimes_WhenCalledRepeatedly() {
        // Given: 초기 점수 100점
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);

        // When: 3번 감쇠 적용
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay(); // 100 * 0.97 = 97
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay(); // 97 * 0.97 = 94.09
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay(); // 94.09 * 0.97 = 91.2673

        // Then: 점수가 지속적으로 감소
        Double finalScore = redisTemplate.opsForZSet().score(SCORE_KEY, "1");
        assertThat(finalScore).isCloseTo(91.2673, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("정상 케이스 - 0점 이하로 감소한 경우")
    void shouldAllowNegativeScore_WhenMultipleDeletesOccur() {
        // Given: 초기 점수 3점
        Long memberId = 1L;
        redisTemplate.opsForZSet().add(SCORE_KEY, memberId.toString(), 3.0);

        // When: 2번 메시지 삭제 (-5점씩)
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, -5.0);
        redisPaperUpdateAdapter.incrementRealtimePopularPaperScore(memberId, -5.0);

        // Then: 음수 점수 허용 (3 - 5 - 5 = -7)
        Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, memberId.toString());
        assertThat(currentScore).isEqualTo(-7.0);
    }
}
