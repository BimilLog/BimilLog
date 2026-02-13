package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostCountSync + RealtimePostSync 로컬 통합 테스트</h2>
 * <p>MySQL + Redis 환경에서 조회수 캐시 버퍼링과 실시간 점수 업데이트를 검증합니다.</p>
 * <p>실행 전 MySQL(bimillogTest) + Redis(6380) 필요</p>
 */
@Tag("local-integration")
@DisplayName("PostCountSync + RealtimePostSync 로컬 통합 테스트")
@SpringBootTest
@ActiveProfiles("local-integration")
class PostCountSyncLocalTest {

    @Autowired
    private PostCountSync postCountSync;

    @Autowired
    private RealtimePostSync realtimePostSync;

    @Autowired
    private RedisPostUpdateAdapter redisPostUpdateAdapter;

    @Autowired
    private RedisPostCounterAdapter redisPostCounterAdapter;

    @Autowired
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final Long TEST_POST_ID = 999999L;
    private static final String TEST_VIEWER_KEY = "ip:127.0.0.1";

    @BeforeEach
    void cleanRedis() {
        // 테스트 관련 키만 정리
        stringRedisTemplate.delete(RedisKey.VIEW_COUNTS_KEY);
        stringRedisTemplate.delete(RedisKey.POST_COUNTERS_KEY);
        stringRedisTemplate.delete(RedisKey.REALTIME_POST_SCORE_KEY);
        // SET NX EX 방식 조회 마킹 키 정리
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":" + TEST_VIEWER_KEY);
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":ip:1.1.1.1");
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":ip:2.2.2.2");
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":m:100");
    }

    // ==================== 조회수 버퍼 ====================

    @Test
    @DisplayName("조회수 - 첫 조회 시 SET NX EX 마킹 + 조회수 버퍼 증가")
    void handlePostViewed_firstView_shouldMarkAndIncrement() {
        // When
        postCountSync.handlePostViewed(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // Then - String 키로 viewer 마킹 확인
        String viewKey = RedisKey.VIEW_PREFIX + TEST_POST_ID + ":" + TEST_VIEWER_KEY;
        Boolean exists = stringRedisTemplate.hasKey(viewKey);
        assertThat(exists).isTrue();

        // Then - Hash 버퍼에 조회수 1 증가 확인
        Map<Long, Long> viewCounts = redisPostUpdateAdapter.getAndClearViewCounts();
        assertThat(viewCounts).containsEntry(TEST_POST_ID, 1L);
    }

    @Test
    @DisplayName("조회수 - 중복 조회 시 조회수 증가하지 않음")
    void handlePostViewed_duplicateView_shouldNotIncrement() {
        // Given - 첫 조회
        postCountSync.handlePostViewed(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // When - 같은 viewerKey로 재조회
        postCountSync.handlePostViewed(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // Then - 조회수는 1만 증가 (중복 방지)
        Map<Long, Long> viewCounts = redisPostUpdateAdapter.getAndClearViewCounts();
        assertThat(viewCounts).containsEntry(TEST_POST_ID, 1L);
    }

    @Test
    @DisplayName("조회수 - 다른 viewerKey는 각각 조회수 증가")
    void handlePostViewed_differentViewers_shouldIncrementEach() {
        // When
        postCountSync.handlePostViewed(TEST_POST_ID, "ip:1.1.1.1");
        postCountSync.handlePostViewed(TEST_POST_ID, "ip:2.2.2.2");
        postCountSync.handlePostViewed(TEST_POST_ID, "m:100");
        waitForAsync();

        // Then - 3명 각각 조회수 증가
        Map<Long, Long> viewCounts = redisPostUpdateAdapter.getAndClearViewCounts();
        assertThat(viewCounts).containsEntry(TEST_POST_ID, 3L);
    }

    // ==================== getAndClear 원자성 ====================

    @Test
    @DisplayName("getAndClear - 조회 후 버퍼가 비워짐")
    void getAndClear_shouldReturnAndDeleteBuffer() {
        // Given - 서로 다른 viewerKey로 2회 조회하여 버퍼에 2 누적
        redisPostUpdateAdapter.markViewedAndIncrement(TEST_POST_ID, "ip:1.1.1.1");
        redisPostUpdateAdapter.markViewedAndIncrement(TEST_POST_ID, "ip:2.2.2.2");

        // When - 첫 번째 호출
        Map<Long, Long> first = redisPostUpdateAdapter.getAndClearViewCounts();
        // When - 두 번째 호출
        Map<Long, Long> second = redisPostUpdateAdapter.getAndClearViewCounts();

        // Then
        assertThat(first).containsEntry(TEST_POST_ID, 2L);
        assertThat(second).isEmpty();
    }

    // ==================== 카운터 캐시 (post:counters) ====================

    @Test
    @DisplayName("좋아요 카운터 캐시 - 캐시글이면 비동기 증감 반영")
    void incrementLikeCounter_shouldIncrementHashField() {
        // Given - SET에 캐시글로 등록
        stringRedisTemplate.opsForSet().add(RedisKey.CACHED_POST_IDS_KEY, TEST_POST_ID.toString());

        // When
        postCountSync.incrementLikeCounter(TEST_POST_ID, 1);
        waitForAsync();

        // Then
        Object value = stringRedisTemplate.opsForHash()
                .get(RedisKey.POST_COUNTERS_KEY, TEST_POST_ID + RedisKey.COUNTER_SUFFIX_LIKE);
        assertThat(value).isNotNull();
        assertThat(Long.parseLong(value.toString())).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 카운터 캐시 - 비캐시글이면 증감하지 않음")
    void incrementLikeCounter_shouldSkip_whenNotCachedPost() {
        // Given - SET에 등록하지 않음 (비캐시글)

        // When
        postCountSync.incrementLikeCounter(TEST_POST_ID, 1);
        waitForAsync();

        // Then
        Object value = stringRedisTemplate.opsForHash()
                .get(RedisKey.POST_COUNTERS_KEY, TEST_POST_ID + RedisKey.COUNTER_SUFFIX_LIKE);
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("댓글 카운터 캐시 - 캐시글이면 비동기 증감 반영")
    void incrementCommentCounter_shouldIncrementHashField() {
        // Given - SET에 캐시글로 등록
        stringRedisTemplate.opsForSet().add(RedisKey.CACHED_POST_IDS_KEY, TEST_POST_ID.toString());

        // When
        postCountSync.incrementCommentCounter(TEST_POST_ID, 1);
        postCountSync.incrementCommentCounter(TEST_POST_ID, 1);
        postCountSync.incrementCommentCounter(TEST_POST_ID, -1);
        waitForAsync();

        // Then
        Object value = stringRedisTemplate.opsForHash()
                .get(RedisKey.POST_COUNTERS_KEY, TEST_POST_ID + RedisKey.COUNTER_SUFFIX_COMMENT);
        assertThat(value).isNotNull();
        assertThat(Long.parseLong(value.toString())).isEqualTo(1L);
    }

    // ==================== 실시간 인기글 점수 ====================

    @Test
    @DisplayName("실시간 점수 - 양수 점수 증가")
    void updateRealtimeScore_positive_shouldIncrementZSet() {
        // When
        realtimePostSync.updateRealtimeScore(TEST_POST_ID, 2.0);
        waitForAsync();

        // Then
        Double score = stringRedisTemplate.opsForZSet()
                .score(RedisKey.REALTIME_POST_SCORE_KEY, String.valueOf(TEST_POST_ID));
        assertThat(score).isEqualTo(2.0);
    }

    @Test
    @DisplayName("실시간 점수 - 여러 이벤트 누적 (조회2 + 추천4 = 6)")
    void updateRealtimeScore_accumulated_shouldSumScores() {
        // When
        realtimePostSync.updateRealtimeScore(TEST_POST_ID, 2.0);  // 조회
        realtimePostSync.updateRealtimeScore(TEST_POST_ID, 4.0);  // 추천
        waitForAsync();

        // Then
        Double score = stringRedisTemplate.opsForZSet()
                .score(RedisKey.REALTIME_POST_SCORE_KEY, String.valueOf(TEST_POST_ID));
        assertThat(score).isEqualTo(6.0);
    }

    @Test
    @DisplayName("실시간 점수 - 추천 취소 시 점수 감소")
    void updateRealtimeScore_negative_shouldDecrementZSet() {
        // Given
        realtimePostSync.updateRealtimeScore(TEST_POST_ID, 4.0);
        waitForAsync();

        // When
        realtimePostSync.updateRealtimeScore(TEST_POST_ID, -4.0);
        waitForAsync();

        // Then
        Double score = stringRedisTemplate.opsForZSet()
                .score(RedisKey.REALTIME_POST_SCORE_KEY, String.valueOf(TEST_POST_ID));
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    @DisplayName("실시간 점수 - 상위 5개 게시글 조회")
    void getRangePostId_shouldReturnTopFiveByScore() {
        // Given - 점수 설정
        realtimePostSync.updateRealtimeScore(1L, 10.0);
        realtimePostSync.updateRealtimeScore(2L, 30.0);
        realtimePostSync.updateRealtimeScore(3L, 20.0);
        realtimePostSync.updateRealtimeScore(4L, 50.0);
        realtimePostSync.updateRealtimeScore(5L, 40.0);
        realtimePostSync.updateRealtimeScore(6L, 5.0);
        waitForAsync();

        // When
        var topPosts = redisRealTimePostAdapter.getRangePostId();

        // Then - 점수 내림차순 상위 5개
        assertThat(topPosts).hasSize(5);
        assertThat(topPosts).containsExactly(4L, 5L, 2L, 3L, 1L);
    }

    // ==================== 헬퍼 ====================

    /**
     * @Async 메서드 완료 대기 (비동기 스레드 풀에서 실행되므로 잠시 대기)
     */
    private void waitForAsync() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
