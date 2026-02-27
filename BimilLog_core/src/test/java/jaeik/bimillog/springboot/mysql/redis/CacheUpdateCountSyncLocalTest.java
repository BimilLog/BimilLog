package jaeik.bimillog.springboot.mysql.redis;

import jaeik.bimillog.domain.post.async.CacheRealtimeSync;
import jaeik.bimillog.domain.post.async.CacheUpdateCountSync;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CacheUpdateCountSync + CacheRealtimeSync 로컬 통합 테스트</h2>
 * <p>MySQL + Redis 환경에서 조회수 캐시 버퍼링과 실시간 점수 업데이트를 검증합니다.</p>
 * <p>실행 전 MySQL(bimillogTest) + Redis(6380) 필요</p>
 */
@Tag("local-integration")
@DisplayName("CacheUpdateCountSync + CacheRealtimeSync 로컬 통합 테스트")
@SpringBootTest
@ActiveProfiles("local-integration")
class CacheUpdateCountSyncLocalTest {

    @Autowired
    private CacheUpdateCountSync cacheUpdateCountSync;

    @Autowired
    private CacheRealtimeSync cacheRealtimeSync;

    @Autowired
    private RedisPostViewAdapter redisPostViewAdapter;

    @Autowired
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final Long TEST_POST_ID = 999999L;
    private static final String TEST_VIEWER_KEY = "ip:127.0.0.1";

    @AfterEach
    void cleanRedis() {
        // 테스트 관련 키만 정리
        stringRedisTemplate.delete(RedisKey.VIEW_COUNTS_KEY);
        stringRedisTemplate.delete(RedisKey.REALTIME_POST_SCORE_KEY);
        // JSON LIST 캐시 키 정리
        stringRedisTemplate.delete(RedisKey.FIRST_PAGE_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_WEEKLY_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_LEGEND_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_NOTICE_JSON_KEY);
        stringRedisTemplate.delete(RedisKey.POST_REALTIME_JSON_KEY);
        // SET NX EX 방식 조회 마킹 키 정리
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":" + TEST_VIEWER_KEY);
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":ip:1.1.1.1");
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":ip:2.2.2.2");
        stringRedisTemplate.delete(RedisKey.VIEW_PREFIX + TEST_POST_ID + ":m:100");
    }

    // ==================== 조회수 버퍼 (CacheRealtimeSync.postDetailCheck) ====================

    @Test
    @DisplayName("조회수 - 첫 조회 시 SET NX EX 마킹 + 조회수 버퍼 증가 + 실시간 점수 증가")
    void postDetailCheck_firstView_shouldMarkAndIncrement() {
        // When
        cacheRealtimeSync.postDetailCheck(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // Then - String 키로 viewer 마킹 확인
        String viewKey = RedisKey.VIEW_PREFIX + TEST_POST_ID + ":" + TEST_VIEWER_KEY;
        Boolean exists = stringRedisTemplate.hasKey(viewKey);
        assertThat(exists).isTrue();

        // Then - Hash 버퍼에 조회수 1 증가 확인
        Map<Long, Long> viewCounts = redisPostViewAdapter.getAndClearViewCounts();
        assertThat(viewCounts).containsEntry(TEST_POST_ID, 1L);

        // Then - 실시간 점수 2.0 증가 확인
        Double score = stringRedisTemplate.opsForZSet()
                .score(RedisKey.REALTIME_POST_SCORE_KEY, String.valueOf(TEST_POST_ID));
        assertThat(score).isEqualTo(2.0);
    }

    @Test
    @DisplayName("조회수 - 중복 조회 시 조회수 증가하지 않음 (실시간 점수는 증가)")
    void postDetailCheck_duplicateView_shouldNotIncrementViewCount() {
        // Given - 첫 조회
        cacheRealtimeSync.postDetailCheck(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // When - 같은 viewerKey로 재조회
        cacheRealtimeSync.postDetailCheck(TEST_POST_ID, TEST_VIEWER_KEY);
        waitForAsync();

        // Then - 조회수는 1만 증가 (중복 방지)
        Map<Long, Long> viewCounts = redisPostViewAdapter.getAndClearViewCounts();
        assertThat(viewCounts).containsEntry(TEST_POST_ID, 1L);
    }

    @Test
    @DisplayName("조회수 - 다른 viewerKey는 각각 조회수 증가")
    void postDetailCheck_differentViewers_shouldIncrementEach() {
        // When
        cacheRealtimeSync.postDetailCheck(TEST_POST_ID, "ip:1.1.1.1");
        cacheRealtimeSync.postDetailCheck(TEST_POST_ID, "ip:2.2.2.2");
        cacheRealtimeSync.postDetailCheck(TEST_POST_ID, "m:100");
        waitForAsync();

        // Then - 3명 각각 조회수 증가
        Map<Long, Long> viewCounts = redisPostViewAdapter.getAndClearViewCounts();
        assertThat(viewCounts).containsEntry(TEST_POST_ID, 3L);
    }

    // ==================== getAndClear 원자성 ====================

    @Test
    @DisplayName("getAndClear - 조회 후 버퍼가 비워짐")
    void getAndClear_shouldReturnAndDeleteBuffer() {
        // Given - 서로 다른 viewerKey로 2회 조회하여 버퍼에 2 누적
        redisPostViewAdapter.markViewedAndIncrement(TEST_POST_ID, "ip:1.1.1.1");
        redisPostViewAdapter.markViewedAndIncrement(TEST_POST_ID, "ip:2.2.2.2");

        // When - 첫 번째 호출
        Map<Long, Long> first = redisPostViewAdapter.getAndClearViewCounts();
        // When - 두 번째 호출
        Map<Long, Long> second = redisPostViewAdapter.getAndClearViewCounts();

        // Then
        assertThat(first).containsEntry(TEST_POST_ID, 2L);
        assertThat(second).isEmpty();
    }

    // ==================== 실시간 인기글 점수 ====================

    @Test
    @DisplayName("실시간 점수 - 양수 점수 증가")
    void updateRealtimeScore_positive_shouldIncrementZSet() {
        // When
        cacheRealtimeSync.updateRealtimeScore(TEST_POST_ID, 2.0);
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
        cacheRealtimeSync.updateRealtimeScore(TEST_POST_ID, 2.0);  // 조회
        cacheRealtimeSync.updateRealtimeScore(TEST_POST_ID, 4.0);  // 추천
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
        cacheRealtimeSync.updateRealtimeScore(TEST_POST_ID, 4.0);
        waitForAsync();

        // When
        cacheRealtimeSync.updateRealtimeScore(TEST_POST_ID, -4.0);
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
        cacheRealtimeSync.updateRealtimeScore(1L, 10.0);
        cacheRealtimeSync.updateRealtimeScore(2L, 30.0);
        cacheRealtimeSync.updateRealtimeScore(3L, 20.0);
        cacheRealtimeSync.updateRealtimeScore(4L, 50.0);
        cacheRealtimeSync.updateRealtimeScore(5L, 40.0);
        cacheRealtimeSync.updateRealtimeScore(6L, 5.0);
        waitForAsync();

        // When
        var topPosts = redisPostRealTimeAdapter.getRangePostId();

        // Then - 점수 내림차순 상위 5개
        assertThat(topPosts).hasSize(5);
        assertThat(topPosts).containsExactly(4L, 5L, 2L, 3L, 1L);
    }

    // ==================== 헬퍼 ====================

    /**
     * Async  메서드 완료 대기 (비동기 스레드 풀에서 실행되므로 잠시 대기)
     */
    private void waitForAsync() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
