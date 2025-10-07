package jaeik.bimillog.adapter.out.redis;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.infrastructure.adapter.out.redis.RedisPostCommandAdapter;
import jaeik.bimillog.testutil.RedisTestHelper;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPostCommandAdapter 통합 테스트</h2>
 * <p>Redis TestContainers를 사용한 실제 Redis 환경에서의 테스트</p>
 * <p>게시글 캐시 명령 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("tc")
@Testcontainers
@Import(TestContainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("tc")
class RedisPostCommandAdapterIntegrationTest {

    @Autowired
    private RedisPostCommandAdapter redisPostCommandAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private PostDetail testPostDetail;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);

        // 테스트 데이터 준비
        testPostDetail = PostDetail.builder()
                .id(1L)
                .title("캐시된 게시글")
                .content("캐시된 내용")
                .viewCount(100)
                .likeCount(50)
                .commentCount(10)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("testMember")
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 인기글 postId 목록 캐시 저장")
    void shouldCachePostIds_WhenValidPostsProvided() {
        // Given
        List<Long> postIds = List.of(1L, 2L, 3L);
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String cacheKey = RedisTestHelper.RedisKeys.postList(cacheType);

        // When
        redisPostCommandAdapter.cachePostIds(cacheType, postIds);

        // Then: Sorted Set에 실제로 저장되었는지 확인
        Set<Object> cachedPostIds = redisTemplate.opsForZSet().reverseRange(cacheKey, 0, -1);
        assertThat(cachedPostIds).hasSize(3);
        assertThat(cachedPostIds).containsExactly("1", "2", "3");

        // 점수 확인 (첫 번째가 가장 높은 점수)
        Double score1 = redisTemplate.opsForZSet().score(cacheKey, "1");
        Double score2 = redisTemplate.opsForZSet().score(cacheKey, "2");
        Double score3 = redisTemplate.opsForZSet().score(cacheKey, "3");
        assertThat(score1).isEqualTo(3.0); // 첫 번째 = 가장 높은 점수
        assertThat(score2).isEqualTo(2.0);
        assertThat(score3).isEqualTo(1.0); // 마지막 = 가장 낮은 점수

        // TTL 확인 (5분)
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L);
    }

    @Test
    @DisplayName("정상 케이스 - 타입별 캐시 삭제 (목록 + 상세)")
    void shouldDeleteCache_WhenValidCacheTypeProvided() {
        // Given: 데이터 준비
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        List<Long> postIds = List.of(1L, 2L);

        // 목록 캐시와 상세 캐시 모두 저장
        redisPostCommandAdapter.cachePostIds(cacheType, postIds);
        redisPostCommandAdapter.cachePostDetail(testPostDetail);
        redisPostCommandAdapter.cachePostDetail(
                PostDetail.builder()
                        .id(2L)
                        .title("게시글 2")
                        .content("내용 2")
                        .viewCount(50)
                        .likeCount(25)
                        .commentCount(5)
                        .isLiked(false)
                        .createdAt(Instant.now())
                        .memberId(2L)
                        .memberName("member2")
                        .build()
        );

        String listKey = RedisTestHelper.RedisKeys.postList(cacheType);
        String detailKey1 = RedisTestHelper.RedisKeys.postDetail(1L);
        String detailKey2 = RedisTestHelper.RedisKeys.postDetail(2L);

        // 저장 확인
        assertThat(redisTemplate.hasKey(listKey)).isTrue();
        assertThat(redisTemplate.hasKey(detailKey1)).isTrue();
        assertThat(redisTemplate.hasKey(detailKey2)).isTrue();

        // When: 타입별 캐시 삭제
        redisPostCommandAdapter.deleteCache(cacheType, null);

        // Then: 목록과 상세 캐시 모두 삭제됨
        assertThat(redisTemplate.hasKey(listKey)).isFalse();
        assertThat(redisTemplate.hasKey(detailKey1)).isFalse();
        assertThat(redisTemplate.hasKey(detailKey2)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 상세 캐시 저장")
    void shouldCachePostDetail_WhenValidPostDetailProvided() {
        // Given
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(testPostDetail.getId());

        // When
        redisPostCommandAdapter.cachePostDetail(testPostDetail);

        // Then: 캐시 키가 존재하는지 확인
        Boolean keyExists = redisTemplate.hasKey(cacheKey);
        assertThat(keyExists).isTrue();

        // TTL 확인 (5분)
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L);

        // RedisTemplate로 직접 조회하여 검증 (QueryAdapter 의존성 제거)
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cached).isNotNull();
        assertThat(cached).isInstanceOf(PostDetail.class);

        PostDetail cachedPost = (PostDetail) cached;
        assertThat(cachedPost.getId()).isEqualTo(1L);
        assertThat(cachedPost.getTitle()).isEqualTo("캐시된 게시글");
        assertThat(cachedPost.getContent()).isEqualTo("캐시된 내용");
        assertThat(cachedPost.getViewCount()).isEqualTo(100);
        assertThat(cachedPost.getMemberName()).isEqualTo("testMember");
    }

    @Test
    @DisplayName("정상 케이스 - 단일 게시글 캐시 삭제")
    void shouldDeleteSinglePostCache_WhenPostIdProvided() {
        // Given: 게시글 상세 캐시 저장
        redisPostCommandAdapter.cachePostDetail(testPostDetail);
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(testPostDetail.getId());
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // When: 단일 게시글 캐시 삭제
        redisPostCommandAdapter.deleteSinglePostCache(testPostDetail.getId());

        // Then: 캐시가 삭제됨
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 특정 게시글의 모든 캐시 삭제")
    void shouldDeleteAllCachesOfPost_WhenPostIdProvided() {
        // Given: 여러 타입의 캐시에 게시글 저장
        Long postId = 1L;

        // WEEKLY와 LEGEND 목록에 추가
        redisPostCommandAdapter.cachePostIds(PostCacheFlag.WEEKLY, List.of(postId));
        redisPostCommandAdapter.cachePostIds(PostCacheFlag.LEGEND, List.of(postId));

        // 상세 캐시 추가
        redisPostCommandAdapter.cachePostDetail(testPostDetail);

        String weeklyKey = RedisTestHelper.RedisKeys.postList(PostCacheFlag.WEEKLY);
        String legendKey = RedisTestHelper.RedisKeys.postList(PostCacheFlag.LEGEND);
        String detailKey = RedisTestHelper.RedisKeys.postDetail(postId);

        // 저장 확인
        assertThat(redisTemplate.opsForZSet().score(weeklyKey, postId.toString())).isNotNull();
        assertThat(redisTemplate.opsForZSet().score(legendKey, postId.toString())).isNotNull();
        assertThat(redisTemplate.hasKey(detailKey)).isTrue();

        // When: 특정 게시글의 모든 캐시 삭제
        redisPostCommandAdapter.deleteCache(null, postId);

        // Then: 상세 캐시와 모든 목록 캐시에서 제거됨
        assertThat(redisTemplate.hasKey(detailKey)).isFalse();
        assertThat(redisTemplate.opsForZSet().score(weeklyKey, postId.toString())).isNull();
        assertThat(redisTemplate.opsForZSet().score(legendKey, postId.toString())).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 증가")
    void shouldIncrementScore_WhenPostIdAndScoreProvided() {
        // Given
        Long postId = 1L;
        double score = 4.0; // 추천 점수
        String scoreKey = "cache:realtime:scores";

        // When: 점수 증가
        redisPostCommandAdapter.incrementRealtimePopularScore(postId, score);

        // Then: Sorted Set에서 점수 확인
        Double currentScore = redisTemplate.opsForZSet().score(scoreKey, postId.toString());
        assertThat(currentScore).isEqualTo(4.0);
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 누적")
    void shouldAccumulateScore_WhenMultipleIncrementsOccur() {
        // Given
        Long postId = 1L;
        String scoreKey = "cache:realtime:scores";

        // When: 여러 번 점수 증가 (조회 2점 + 댓글 3점 + 추천 4점)
        redisPostCommandAdapter.incrementRealtimePopularScore(postId, 2.0); // 조회
        redisPostCommandAdapter.incrementRealtimePopularScore(postId, 3.0); // 댓글
        redisPostCommandAdapter.incrementRealtimePopularScore(postId, 4.0); // 추천

        // Then: 누적 점수 확인
        Double currentScore = redisTemplate.opsForZSet().score(scoreKey, postId.toString());
        assertThat(currentScore).isEqualTo(9.0); // 2 + 3 + 4
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 감쇠 적용 (Lua 스크립트)")
    void shouldApplyDecay_WhenScoreDecayInvoked() {
        // Given: 여러 게시글에 초기 점수 설정
        String scoreKey = "cache:realtime:scores";
        redisTemplate.opsForZSet().add(scoreKey, "1", 10.0);
        redisTemplate.opsForZSet().add(scoreKey, "2", 5.0);
        redisTemplate.opsForZSet().add(scoreKey, "3", 2.0);

        // When: 감쇠 적용 (0.9배)
        redisPostCommandAdapter.applyRealtimePopularScoreDecay();

        // Then: 점수가 0.9배로 감소
        Double score1 = redisTemplate.opsForZSet().score(scoreKey, "1");
        Double score2 = redisTemplate.opsForZSet().score(scoreKey, "2");
        Double score3 = redisTemplate.opsForZSet().score(scoreKey, "3");

        assertThat(score1).isEqualTo(9.0);  // 10 * 0.9
        assertThat(score2).isEqualTo(4.5);  // 5 * 0.9
        assertThat(score3).isEqualTo(1.8);  // 2 * 0.9
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 감쇠 시 임계값 이하 제거")
    void shouldRemovePostsBelowThreshold_WhenScoreDecayApplied() {
        // Given: 임계값(1.0) 근처의 점수 설정
        String scoreKey = "cache:realtime:scores";
        redisTemplate.opsForZSet().add(scoreKey, "1", 10.0);
        redisTemplate.opsForZSet().add(scoreKey, "2", 1.5);  // 감쇠 후 1.35 (유지)
        redisTemplate.opsForZSet().add(scoreKey, "3", 1.1);  // 감쇠 후 0.99 (제거)
        redisTemplate.opsForZSet().add(scoreKey, "4", 0.8);  // 감쇠 후 0.72 (제거)

        // 초기 크기 확인
        Long initialSize = redisTemplate.opsForZSet().size(scoreKey);
        assertThat(initialSize).isEqualTo(4);

        // When: 감쇠 적용
        redisPostCommandAdapter.applyRealtimePopularScoreDecay();

        // Then: 임계값(1.0) 이하의 게시글은 제거됨
        Long finalSize = redisTemplate.opsForZSet().size(scoreKey);
        assertThat(finalSize).isEqualTo(2); // 1번과 2번만 남음

        // 남아있는 게시글 확인
        Set<Object> remainingPosts = redisTemplate.opsForZSet().range(scoreKey, 0, -1);
        assertThat(remainingPosts).containsExactlyInAnyOrder("1", "2");

        // 점수 확인
        Double score1 = redisTemplate.opsForZSet().score(scoreKey, "1");
        Double score2 = redisTemplate.opsForZSet().score(scoreKey, "2");
        assertThat(score1).isEqualTo(9.0);   // 10 * 0.9
        assertThat(score2).isEqualTo(1.35);  // 1.5 * 0.9

        // 제거된 게시글 확인
        assertThat(redisTemplate.opsForZSet().score(scoreKey, "3")).isNull();
        assertThat(redisTemplate.opsForZSet().score(scoreKey, "4")).isNull();
    }

    @Test
    @DisplayName("경계값 - 빈 목록으로 캐시 저장 시도")
    void shouldHandleEmptyList_WhenCachingPostIds() {
        // Given
        List<Long> emptyPostIds = List.of();
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String cacheKey = RedisTestHelper.RedisKeys.postList(cacheType);

        // When: 빈 목록으로 캐시 저장 (아무 동작도 하지 않아야 함)
        redisPostCommandAdapter.cachePostIds(cacheType, emptyPostIds);

        // Then: 캐시 키가 생성되지 않음
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }
}
