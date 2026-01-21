package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
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
 * @version 2.7.0
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
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        List<PostSimpleDetail> posts = List.of(
                toSimpleDetail(testPostDetail1),
                toSimpleDetail(testPostDetail2)
        );
        redisSimplePostAdapter.cachePosts(type, posts);

        // When: HGETALL로 조회
        Map<Long, PostSimpleDetail> result = redisSimplePostAdapter.getAllCachedPosts(type);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.get(2L).getTitle()).isEqualTo("두 번째 게시글");
    }

    @Test
    @DisplayName("정상 케이스 - 빈 Hash 조회 시 빈 Map 반환")
    void shouldReturnEmptyMap_WhenCacheEmpty() {
        // Given: 캐시 없음
        PostCacheFlag type = PostCacheFlag.WEEKLY;

        // When
        Map<Long, PostSimpleDetail> result = redisSimplePostAdapter.getAllCachedPosts(type);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - Hash 캐시 저장 및 TTL 확인")
    void shouldCachePosts_WithCorrectTTL() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));

        // When: 저장
        redisSimplePostAdapter.cachePosts(type, posts);

        // Then: Hash 키 존재 및 TTL 확인
        String hashKey = RedisPostKeys.getSimplePostHashKey(type);
        assertThat(redisTemplate.hasKey(hashKey)).isTrue();

        Long ttl = redisTemplate.getExpire(hashKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L); // 5분 TTL

        // Hash 필드 확인
        Object cachedValue = redisTemplate.opsForHash().get(hashKey, "1");
        assertThat(cachedValue).isInstanceOf(PostSimpleDetail.class);
    }

    @Test
    @DisplayName("정상 케이스 - 여러 게시글 Hash 캐시 저장 (HMSET)")
    void shouldCachePosts_WhenMultiplePostsProvided() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME;
        List<PostSimpleDetail> posts = List.of(
                toSimpleDetail(testPostDetail1),
                toSimpleDetail(testPostDetail2),
                toSimpleDetail(testPostDetail3)
        );

        // When: 여러 개 저장 (HMSET)
        redisSimplePostAdapter.cachePosts(type, posts);

        // Then: Hash 키 존재 확인
        String hashKey = RedisPostKeys.getSimplePostHashKey(type);
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
    @DisplayName("정상 케이스 - 단일 캐시 삭제 (HDEL)")
    void shouldRemovePostFromCache() {
        // Given: 모든 타입에 게시글 캐시 저장
        Long postId = 1L;
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));

        for (PostCacheFlag type : PostCacheFlag.values()) {
            redisSimplePostAdapter.cachePosts(type, posts);
        }

        // 저장 확인
        for (PostCacheFlag type : PostCacheFlag.values()) {
            String hashKey = RedisPostKeys.getSimplePostHashKey(type);
            assertThat(redisTemplate.opsForHash().hasKey(hashKey, "1")).isTrue();
        }

        // When: 삭제
        redisSimplePostAdapter.removePostFromCache(postId);

        // Then: 모든 타입에서 삭제됨
        for (PostCacheFlag type : PostCacheFlag.values()) {
            String hashKey = RedisPostKeys.getSimplePostHashKey(type);
            assertThat(redisTemplate.opsForHash().hasKey(hashKey, "1")).isFalse();
        }
    }

    @Test
    @DisplayName("정상 케이스 - 특정 타입의 캐시만 삭제")
    void shouldRemovePostFromCache_SpecificType() {
        // Given: WEEKLY와 LEGEND에 게시글 캐시 저장
        Long postId = 1L;
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));

        redisSimplePostAdapter.cachePosts(PostCacheFlag.WEEKLY, posts);
        redisSimplePostAdapter.cachePosts(PostCacheFlag.LEGEND, posts);

        // When: WEEKLY만 삭제
        redisSimplePostAdapter.removePostFromCache(PostCacheFlag.WEEKLY, postId);

        // Then: WEEKLY는 삭제, LEGEND는 유지
        String weeklyHashKey = RedisPostKeys.getSimplePostHashKey(PostCacheFlag.WEEKLY);
        String legendHashKey = RedisPostKeys.getSimplePostHashKey(PostCacheFlag.LEGEND);

        assertThat(redisTemplate.opsForHash().hasKey(weeklyHashKey, "1")).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(legendHashKey, "1")).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - Hash 키 생성 확인")
    void shouldGenerateHashKey() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME;

        // When
        String hashKey = RedisPostKeys.getSimplePostHashKey(type);

        // Then: Hash 키 형식 확인
        assertThat(hashKey).isEqualTo("post:realtime:simple");
    }

    @Test
    @DisplayName("정상 케이스 - 각 타입별 Hash 키 생성")
    void shouldGenerateHashKeysForAllTypes() {
        // When & Then
        assertThat(RedisPostKeys.getSimplePostHashKey(PostCacheFlag.WEEKLY))
                .isEqualTo("post:weekly:simple");
        assertThat(RedisPostKeys.getSimplePostHashKey(PostCacheFlag.LEGEND))
                .isEqualTo("post:legend:simple");
        assertThat(RedisPostKeys.getSimplePostHashKey(PostCacheFlag.NOTICE))
                .isEqualTo("post:notice:simple");
        assertThat(RedisPostKeys.getSimplePostHashKey(PostCacheFlag.REALTIME))
                .isEqualTo("post:realtime:simple");
    }

    @Test
    @DisplayName("정상 케이스 - Hash 전체 PER 갱신 필요 여부 확인")
    void shouldCheckHashRefreshNeeded() {
        // Given: 캐시가 없을 때
        PostCacheFlag type = PostCacheFlag.WEEKLY;

        // When & Then: 키가 없으면 false
        assertThat(redisSimplePostAdapter.shouldRefreshHash(type)).isFalse();

        // Given: 캐시 저장 (TTL 300초)
        List<PostSimpleDetail> posts = List.of(toSimpleDetail(testPostDetail1));
        redisSimplePostAdapter.cachePosts(type, posts);

        // When & Then: TTL이 충분하면 false
        assertThat(redisSimplePostAdapter.shouldRefreshHash(type)).isFalse();
    }
}
