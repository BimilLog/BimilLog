package jaeik.bimillog.adapter.out.redis.paper;

import jaeik.bimillog.infrastructure.adapter.out.redis.paper.RedisPaperKeys;
import jaeik.bimillog.infrastructure.adapter.out.redis.paper.RedisPaperUpdateAdapter;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPaperUpdateAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 롤링페이퍼 캐시 갱신 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPaperUpdateAdapterIntegrationTest {

    @Autowired
    private RedisPaperUpdateAdapter redisPaperUpdateAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SCORE_KEY = RedisPaperKeys.REALTIME_PAPER_SCORE_KEY;

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

        assertThat(score1).isEqualTo(9.5);  // 10 * 0.95
        assertThat(score2).isEqualTo(4.75); // 5 * 0.95
        assertThat(score3).isEqualTo(1.9);  // 2 * 0.95
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 롤링페이퍼 점수 감쇠 시 임계값 이하 제거")
    void shouldRemovePapersBelowThreshold_WhenScoreDecayApplied() {
        // Given: 임계값(1.0) 근처의 점수 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 10.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 1.5);  // 감쇠 후 1.425 (유지)
        redisTemplate.opsForZSet().add(SCORE_KEY, "3", 1.05); // 감쇠 후 0.9975 (제거)
        redisTemplate.opsForZSet().add(SCORE_KEY, "4", 0.8);  // 감쇠 후 0.76 (제거)

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(initialSize).isEqualTo(4);

        // When: 감쇠 적용
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();

        // Then: 임계값(1.0) 이하의 롤링페이퍼는 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isEqualTo(2); // 1번과 2번만 남음

        // 남아있는 롤링페이퍼 확인
        Set<Object> remainingPapers = redisTemplate.opsForZSet().range(SCORE_KEY, 0, -1);
        assertThat(remainingPapers).containsExactlyInAnyOrder("1", "2");

        // 점수 확인
        Double score1 = redisTemplate.opsForZSet().score(SCORE_KEY, "1");
        Double score2 = redisTemplate.opsForZSet().score(SCORE_KEY, "2");
        assertThat(score1).isEqualTo(9.5);    // 10 * 0.95
        assertThat(score2).isEqualTo(1.425);  // 1.5 * 0.95

        // 제거된 롤링페이퍼 확인
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "3")).isNull();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "4")).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 감쇠 후 모든 항목이 임계값 이하인 경우")
    void shouldRemoveAllPapers_WhenAllScoresAreBelowThreshold() {
        // Given: 모든 점수가 임계값 근처
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 1.05); // 감쇠 후 0.9975 (제거)
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 0.9);  // 감쇠 후 0.855 (제거)
        redisTemplate.opsForZSet().add(SCORE_KEY, "3", 0.8);  // 감쇠 후 0.76 (제거)

        // When: 감쇠 적용
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();

        // Then: 모든 항목 제거
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isZero();
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
    @DisplayName("정상 케이스 - 점수가 정확히 임계값인 경우")
    void shouldRemovePaper_WhenScoreEqualsThreshold() {
        // Given: 점수가 정확히 1.0
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 1.0);

        // When: 감쇠 적용 (1.0 * 0.95 = 0.95)
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay();

        // Then: 0.95 < 1.0 이므로 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(SCORE_KEY);
        assertThat(finalSize).isZero();
        assertThat(redisTemplate.opsForZSet().score(SCORE_KEY, "1")).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 번 감쇠 적용")
    void shouldApplyDecayMultipleTimes_WhenCalledRepeatedly() {
        // Given: 초기 점수 100점
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);

        // When: 3번 감쇠 적용
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay(); // 100 * 0.95 = 95
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay(); // 95 * 0.95 = 90.25
        redisPaperUpdateAdapter.applyRealtimePopularPaperScoreDecay(); // 90.25 * 0.95 = 85.7375

        // Then: 점수가 지속적으로 감소
        Double finalScore = redisTemplate.opsForZSet().score(SCORE_KEY, "1");
        assertThat(finalScore).isCloseTo(85.7375, org.assertj.core.data.Offset.offset(0.0001));
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
