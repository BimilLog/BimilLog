package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.infrastructure.redis.RedisKey;
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
 * <p>MySQL + Redis 환경에서 조회수/추천수/댓글수 캐시 버퍼링과 실시간 점수 업데이트를 검증합니다.</p>
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
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final Long TEST_POST_ID = 999999L;
    private static final String TEST_VIEWER_KEY = "ip:127.0.0.1";

    @BeforeEach
    void cleanRedis() {
        // 테스트 관련 키만 정리
        stringRedisTemplate.delete(RedisKey.VIEW_COUNTS_KEY);
        stringRedisTemplate.delete(RedisKey.LIKE_COUNTS_KEY);
        stringRedisTemplate.delete(RedisKey.COMMENT_COUNTS_KEY);
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID);
        stringRedisTemplate.delete(RedisKey.REALTIME_POST_SCORE_KEY);
    }

    // ==================== 조회수 버퍼 ====================

    @Test
    @DisplayName("조회수 - 첫 조회 시 Redis SET 마킹 + 조회수 버퍼 증가")
    void handlePostViewed_firstView_shouldMarkAndIncrement() {
        // When
        postCountSync.handlePostViewed(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // Then - SET에 viewer 마킹 확인
        Boolean isMember = stringRedisTemplate.opsForSet()
                .isMember(RedisKey.VIEW_PREFIX + TEST_POST_ID, TEST_VIEWER_KEY);
        assertThat(isMember).isTrue();

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

    // ==================== 추천수 버퍼 ====================

    @Test
    @DisplayName("추천수 - 추천 시 Redis Hash 버퍼에 +1 증가")
    void incrementLike_shouldBufferInRedis() {
        // When
        postCountSync.incrementLikeWithFallback(TEST_POST_ID, 1);
        waitForAsync();

        // Then
        Map<Long, Long> likeCounts = redisPostUpdateAdapter.getAndClearLikeCounts();
        assertThat(likeCounts).containsEntry(TEST_POST_ID, 1L);
    }

    @Test
    @DisplayName("추천수 - 추천 취소 시 Redis Hash 버퍼에 -1 감소")
    void decrementLike_shouldBufferNegativeInRedis() {
        // When
        postCountSync.incrementLikeWithFallback(TEST_POST_ID, -1);
        waitForAsync();

        // Then
        Map<Long, Long> likeCounts = redisPostUpdateAdapter.getAndClearLikeCounts();
        assertThat(likeCounts).containsEntry(TEST_POST_ID, -1L);
    }

    @Test
    @DisplayName("추천수 - 추천/취소 반복 시 버퍼 합산")
    void likeAndUnlike_shouldAccumulate() {
        // When - 추천 3회, 취소 1회 = 순증 +2
        postCountSync.incrementLikeWithFallback(TEST_POST_ID, 1);
        postCountSync.incrementLikeWithFallback(TEST_POST_ID, 1);
        postCountSync.incrementLikeWithFallback(TEST_POST_ID, 1);
        postCountSync.incrementLikeWithFallback(TEST_POST_ID, -1);
        waitForAsync();

        // Then
        Map<Long, Long> likeCounts = redisPostUpdateAdapter.getAndClearLikeCounts();
        assertThat(likeCounts).containsEntry(TEST_POST_ID, 2L);
    }

    // ==================== 댓글수 버퍼 (직접 어댑터 호출) ====================

    @Test
    @DisplayName("댓글수 - 댓글 작성 시 Redis Hash 버퍼에 +1 증가")
    void incrementComment_shouldBufferInRedis() {
        // When
        redisPostUpdateAdapter.incrementCommentBuffer(TEST_POST_ID, 1);

        // Then
        Map<Long, Long> commentCounts = redisPostUpdateAdapter.getAndClearCommentCounts();
        assertThat(commentCounts).containsEntry(TEST_POST_ID, 1L);
    }

    @Test
    @DisplayName("댓글수 - 댓글 삭제 시 Redis Hash 버퍼에 -1 감소")
    void decrementComment_shouldBufferNegativeInRedis() {
        // When
        redisPostUpdateAdapter.incrementCommentBuffer(TEST_POST_ID, -1);

        // Then
        Map<Long, Long> commentCounts = redisPostUpdateAdapter.getAndClearCommentCounts();
        assertThat(commentCounts).containsEntry(TEST_POST_ID, -1L);
    }

    // ==================== getAndClear 원자성 ====================

    @Test
    @DisplayName("getAndClear - 조회 후 버퍼가 비워짐")
    void getAndClear_shouldReturnAndDeleteBuffer() {
        // Given
        redisPostUpdateAdapter.incrementViewCount(TEST_POST_ID);
        redisPostUpdateAdapter.incrementViewCount(TEST_POST_ID);

        // When - 첫 번째 호출
        Map<Long, Long> first = redisPostUpdateAdapter.getAndClearViewCounts();
        // When - 두 번째 호출
        Map<Long, Long> second = redisPostUpdateAdapter.getAndClearViewCounts();

        // Then
        assertThat(first).containsEntry(TEST_POST_ID, 2L);
        assertThat(second).isEmpty();
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
