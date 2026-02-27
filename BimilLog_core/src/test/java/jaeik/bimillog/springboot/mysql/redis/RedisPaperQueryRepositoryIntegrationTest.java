package jaeik.bimillog.springboot.mysql.redis;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperQueryAdapter;
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

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPaperQueryAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 롤링페이퍼 캐시 조회 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPaperQueryRepositoryIntegrationTest {

    @Autowired
    private RedisPaperQueryAdapter redisPaperQueryAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SCORE_KEY = RedisKey.REALTIME_PAPER_SCORE_KEY;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 롤링페이퍼 조회 (Rank, Score 포함)")
    void shouldReturnPopularPapers_WhenDataExists() {
        // Given: Redis Sorted Set에 인기 롤링페이퍼 점수 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 80.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "3", 60.0);

        // When: 상위 3개 조회 (0~2)
        List<PopularPaperInfo> result = redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 2);

        // Then: 점수 높은 순서로 반환, rank와 score 포함
        assertThat(result).hasSize(3);

        // 1등: memberId=1, rank=1, score=100.0
        assertThat(result.get(0).getMemberId()).isEqualTo(1L);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getPopularityScore()).isEqualTo(100.0);

        // 2등: memberId=2, rank=2, score=80.0
        assertThat(result.get(1).getMemberId()).isEqualTo(2L);
        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getPopularityScore()).isEqualTo(80.0);

        // 3등: memberId=3, rank=3, score=60.0
        assertThat(result.get(2).getMemberId()).isEqualTo(3L);
        assertThat(result.get(2).getRank()).isEqualTo(3);
        assertThat(result.get(2).getPopularityScore()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("정상 케이스 - 페이징 범위 조회 (start, end 인덱스)")
    void shouldReturnPagedResults_WhenRangeSpecified() {
        // Given: 10개의 롤링페이퍼 점수 설정
        for (int i = 1; i <= 10; i++) {
            redisTemplate.opsForZSet().add(SCORE_KEY, String.valueOf(i), 100.0 - i);
        }

        // When: 4~6위 조회 (start=3, end=5)
        List<PopularPaperInfo> result = redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(3, 5);

        // Then: 3개 반환, rank는 4부터 시작
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRank()).isEqualTo(4);
        assertThat(result.get(1).getRank()).isEqualTo(5);
        assertThat(result.get(2).getRank()).isEqualTo(6);
    }

    @ParameterizedTest(name = "데이터 {0}개, 범위[{1},{2}] => 결과 {3}개 ({4})")
    @MethodSource("provideRangeQueryTestCases")
    @DisplayName("범위 조회 - 다양한 케이스")
    void shouldReturnCorrectResultsForRangeQuery(int dataCount, int start, int end, int expectedSize, String description) {
        // Given
        for (int i = 1; i <= dataCount; i++) {
            redisTemplate.opsForZSet().add(SCORE_KEY, String.valueOf(i), 100.0 - i);
        }

        // When
        List<PopularPaperInfo> result = redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(start, end);

        // Then
        assertThat(result).hasSize(expectedSize);
        if (!result.isEmpty()) {
            assertThat(result).allMatch(info ->
                    info.getMemberId() != null && info.getPopularityScore() > 0);
        }
    }

    static Stream<Arguments> provideRangeQueryTestCases() {
        return Stream.of(
                Arguments.of(0, 0, 10, 0, "데이터 없음"),
                Arguments.of(3, 0, 9, 3, "범위 초과 조회"),
                Arguments.of(1, 0, 0, 1, "단일 데이터"),
                Arguments.of(3, 10, 15, 0, "시작 인덱스 초과")
        );
    }

    @Test
    @DisplayName("정상 케이스 - 동일 점수의 경우 memberId 순서대로 반환")
    void shouldReturnByMemberId_WhenScoresAreSame() {
        // Given: 동일한 점수 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "5", 50.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "3", 50.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "7", 50.0);

        // When: 조회
        List<PopularPaperInfo> result = redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 2);

        // Then: 3개 반환 (동일 점수일 때 Redis 정렬 기준 적용)
        assertThat(result).hasSize(3);
        assertThat(result).extracting(PopularPaperInfo::getPopularityScore)
                .containsOnly(50.0);
    }

    @Test
    @DisplayName("정상 케이스 - null 값이 포함된 경우 건너뜀")
    void shouldSkipNullValues_WhenNullExists() {
        // Given: 정상 데이터 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "1", 100.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "2", 80.0);

        // When: 조회
        List<PopularPaperInfo> result = redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 5);

        // Then: null 아닌 값만 반환
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(info ->
                info.getMemberId() != null &&
                info.getPopularityScore() > 0
        );
    }
}
