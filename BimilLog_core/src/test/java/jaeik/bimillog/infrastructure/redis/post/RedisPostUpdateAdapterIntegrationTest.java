package jaeik.bimillog.infrastructure.redis.post;

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
 * <h2>RedisPostUpdateAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 게시글 캐시 수정 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPostUpdateAdapterIntegrationTest {

    @Autowired
    private RedisPostUpdateAdapter redisPostUpdateAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 증가")
    void shouldIncrementScore_WhenPostIdAndScoreProvided() {
        // Given
        Long postId = 1L;
        double score = 4.0; // 추천 점수
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;

        // When: 점수 증가
        redisPostUpdateAdapter.incrementRealtimePopularScore(postId, score);

        // Then: Sorted Set에서 점수 확인
        Double currentScore = redisTemplate.opsForZSet().score(scoreKey, postId.toString());
        assertThat(currentScore).isEqualTo(4.0);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 누적")
    void shouldAccumulateScore_WhenMultipleIncrementsOccur() {
        // Given
        Long postId = 1L;
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;

        // When: 여러 번 점수 증가 (조회 2점 + 댓글 3점 + 추천 4점)
        redisPostUpdateAdapter.incrementRealtimePopularScore(postId, 2.0); // 조회
        redisPostUpdateAdapter.incrementRealtimePopularScore(postId, 3.0); // 댓글
        redisPostUpdateAdapter.incrementRealtimePopularScore(postId, 4.0); // 추천

        // Then: 누적 점수 확인
        Double currentScore = redisTemplate.opsForZSet().score(scoreKey, postId.toString());
        assertThat(currentScore).isEqualTo(9.0); // 2 + 3 + 4
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 감쇠 적용 (Lua 스크립트)")
    void shouldApplyDecay_WhenScoreDecayInvoked() {
        // Given: 여러 게시글에 초기 점수 설정
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;
        redisTemplate.opsForZSet().add(scoreKey, "1", 10.0);
        redisTemplate.opsForZSet().add(scoreKey, "2", 5.0);
        redisTemplate.opsForZSet().add(scoreKey, "3", 2.0);

        // When: 감쇠 적용 (0.9배)
        redisPostUpdateAdapter.applyRealtimePopularScoreDecay();

        // Then: 점수가 0.9배로 감소
        Double score1 = redisTemplate.opsForZSet().score(scoreKey, "1");
        Double score2 = redisTemplate.opsForZSet().score(scoreKey, "2");
        Double score3 = redisTemplate.opsForZSet().score(scoreKey, "3");

        assertThat(score1).isEqualTo(9.7);  // 10 * 0.97
        assertThat(score2).isEqualTo(4.85);  // 5 * 0.97
        assertThat(score3).isEqualTo(1.94);  // 2 * 0.97
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 감쇠 시 임계값 이하 제거")
    void shouldRemovePostsBelowThreshold_WhenScoreDecayApplied() {
        // Given: 임계값(1.0) 근처의 점수 설정
        String scoreKey = RedisPostKeys.REALTIME_POST_SCORE_KEY;
        redisTemplate.opsForZSet().add(scoreKey, "1", 10.0);
        redisTemplate.opsForZSet().add(scoreKey, "2", 1.5);  // 감쇠 후(유지)
        redisTemplate.opsForZSet().add(scoreKey, "3", 1.02);  // 감쇠 후 (제거)
        redisTemplate.opsForZSet().add(scoreKey, "4", 0.8);  // 감쇠 후 0.72 (제거)

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(scoreKey);
        assertThat(initialSize).isEqualTo(4);

        // When: 감쇠 적용
        redisPostUpdateAdapter.applyRealtimePopularScoreDecay();

        // Then: 임계값(1.0) 이하의 게시글은 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(scoreKey);
        assertThat(finalSize).isEqualTo(2); // 1번과 2번만 남음

        // 남아있는 게시글 확인
        Set<Object> remainingPosts = redisTemplate.opsForZSet().range(scoreKey, 0, -1);
        assertThat(remainingPosts).containsExactlyInAnyOrder("1", "2");

        // 점수 확인
        Double score1 = redisTemplate.opsForZSet().score(scoreKey, "1");
        Double score2 = redisTemplate.opsForZSet().score(scoreKey, "2");
        assertThat(score1).isEqualTo(9.7);   // 10 * 0.97
        assertThat(score2).isEqualTo(1.455);  // 1.5 * 0.97

        // 제거된 게시글 확인
        assertThat(redisTemplate.opsForZSet().score(scoreKey, "3")).isNull();
        assertThat(redisTemplate.opsForZSet().score(scoreKey, "4")).isNull();
    }
}
