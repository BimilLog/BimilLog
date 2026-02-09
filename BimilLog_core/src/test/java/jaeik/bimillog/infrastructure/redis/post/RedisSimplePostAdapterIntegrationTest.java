package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.redis.RedisKey;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisSimplePostAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 Hash 기반 게시글 캐시 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisSimplePostAdapterIntegrationTest {

    @Autowired
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private PostDetail testPostDetail1;
    private PostDetail testPostDetail2;
    private PostDetail testPostDetail3;

    /**
     * <h3>PostDetail을 PostSimpleDetail로 변환</h3>
     */
    private PostSimpleDetail toSimpleDetail(PostDetail detail) {
        return PostSimpleDetail.builder()
                .id(detail.getId())
                .title(detail.getTitle())
                .viewCount(detail.getViewCount())
                .likeCount(detail.getLikeCount())
                .commentCount(detail.getCommentCount())
                .createdAt(detail.getCreatedAt())
                .memberId(detail.getMemberId())
                .memberName(detail.getMemberName())
                .build();
    }

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);

        // 테스트 데이터 준비
        testPostDetail1 = PostDetail.builder()
                .id(1L)
                .title("첫 번째 게시글")
                .content("첫 번째 내용")
                .viewCount(100)
                .likeCount(50)
                .commentCount(10)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("member1")
                .build();

        testPostDetail2 = PostDetail.builder()
                .id(2L)
                .title("두 번째 게시글")
                .content("두 번째 내용")
                .viewCount(200)
                .likeCount(100)
                .commentCount(20)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(2L)
                .memberName("member2")
                .build();

        testPostDetail3 = PostDetail.builder()
                .id(3L)
                .title("세 번째 게시글")
                .content("세 번째 내용")
                .viewCount(150)
                .likeCount(75)
                .commentCount(15)
                .isLiked(false)
                .createdAt(Instant.now())
                .memberId(3L)
                .memberName("member3")
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - HGETALL로 Hash 전체 캐시 조회")
    void shouldReturnAllCachedPosts_WhenCacheExists() {
        // Given: Hash 캐시로 저장
        String hashKey = RedisKey.WEEKLY_SIMPLE_KEY;
        List<PostSimpleDetail> posts = List.of(
                toSimpleDetail(testPostDetail1),
                toSimpleDetail(testPostDetail2)
        );
        redisSimplePostAdapter.cachePostsWithTtl(hashKey, posts, RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND);

        // When: HGETALL로 조회
        Map<Long, PostSimpleDetail> result = redisSimplePostAdapter.getAllCachedPosts(hashKey);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.get(2L).getTitle()).isEqualTo("두 번째 게시글");
    }

    @Test
    @DisplayName("정상 케이스 - 빈 Hash 조회 시 빈 Map 반환")
    void shouldReturnEmptyMap_WhenCacheEmpty() {
        // Given: 캐시 없음

        // When
        Map<Long, PostSimpleDetail> result = redisSimplePostAdapter.getAllCachedPosts(RedisKey.WEEKLY_SIMPLE_KEY);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - Hash 캐시 저장 및 TTL 확인")
    void shouldCachePosts_WithCorrectTTL() {
        // Given
        String hashKey = RedisKey.WEEKLY_SIMPLE_KEY;
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));

        // When: 저장
        redisSimplePostAdapter.cachePostsWithTtl(hashKey, posts, RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND);

        // Then: Hash 키 존재 및 TTL 확인
        assertThat(redisTemplate.hasKey(hashKey)).isTrue();

        Long ttl = redisTemplate.getExpire(hashKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(88190L, 88200L); // 24시간 30분 TTL

        // Hash 필드 확인
        Object cachedValue = redisTemplate.opsForHash().get(hashKey, "1");
        assertThat(cachedValue).isInstanceOf(PostSimpleDetail.class);
    }

    @Test
    @DisplayName("정상 케이스 - 여러 게시글 Hash 캐시 저장 (HMSET)")
    void shouldCachePosts_WhenMultiplePostsProvided() {
        // Given
        String hashKey = RedisKey.REALTIME_SIMPLE_KEY;
        List<PostSimpleDetail> posts = List.of(
                toSimpleDetail(testPostDetail1),
                toSimpleDetail(testPostDetail2),
                toSimpleDetail(testPostDetail3)
        );

        // When: 여러 개 저장 (HMSET) - 영구 저장
        redisSimplePostAdapter.cachePostsWithTtl(hashKey, posts, null);

        // Then: Hash 키 존재 확인
        assertThat(redisTemplate.hasKey(hashKey)).isTrue();

        // Hash 필드 개수 확인
        Long hashSize = redisTemplate.opsForHash().size(hashKey);
        assertThat(hashSize).isEqualTo(3);

        // 각 필드 존재 확인
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, "1")).isTrue();
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, "2")).isTrue();
        assertThat(redisTemplate.opsForHash().hasKey(hashKey, "3")).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 단일 캐시 삭제 (HDEL) - 모든 featured 캐시에서 삭제")
    void shouldRemovePostFromCache() {
        // Given: 모든 featured 타입에 게시글 캐시 저장
        Long postId = 1L;
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));

        String[] hashKeys = {RedisKey.WEEKLY_SIMPLE_KEY, RedisKey.LEGEND_SIMPLE_KEY, RedisKey.NOTICE_SIMPLE_KEY};
        for (String hashKey : hashKeys) {
            redisSimplePostAdapter.cachePostsWithTtl(hashKey, posts, null);
        }

        // 저장 확인
        for (String hashKey : hashKeys) {
            assertThat(redisTemplate.opsForHash().hasKey(hashKey, "1")).isTrue();
        }

        // When: 삭제
        redisSimplePostAdapter.removePostFromCache(postId);

        // Then: 모든 타입에서 삭제됨
        for (String hashKey : hashKeys) {
            assertThat(redisTemplate.opsForHash().hasKey(hashKey, "1")).isFalse();
        }
    }

    @Test
    @DisplayName("정상 케이스 - 특정 타입의 캐시만 삭제")
    void shouldRemovePostFromCache_SpecificType() {
        // Given: WEEKLY와 LEGEND에 게시글 캐시 저장
        Long postId = 1L;
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));

        redisSimplePostAdapter.cachePostsWithTtl(RedisKey.WEEKLY_SIMPLE_KEY, posts, RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND);
        redisSimplePostAdapter.cachePostsWithTtl(RedisKey.LEGEND_SIMPLE_KEY, posts, RedisKey.POST_CACHE_TTL_WEEKLY_LEGEND);

        // When: WEEKLY만 삭제
        redisSimplePostAdapter.removePostFromCache(RedisKey.WEEKLY_SIMPLE_KEY, postId);

        // Then: WEEKLY는 삭제, LEGEND는 유지
        assertThat(redisTemplate.opsForHash().hasKey(RedisKey.WEEKLY_SIMPLE_KEY, "1")).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(RedisKey.LEGEND_SIMPLE_KEY, "1")).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 각 타입별 Hash 키 확인")
    void shouldHaveCorrectHashKeys() {
        // When & Then
        assertThat(RedisKey.WEEKLY_SIMPLE_KEY).isEqualTo("post:weekly:simple");
        assertThat(RedisKey.LEGEND_SIMPLE_KEY).isEqualTo("post:legend:simple");
        assertThat(RedisKey.NOTICE_SIMPLE_KEY).isEqualTo("post:notice:simple");
        assertThat(RedisKey.REALTIME_SIMPLE_KEY).isEqualTo("post:realtime:simple");
    }

}
