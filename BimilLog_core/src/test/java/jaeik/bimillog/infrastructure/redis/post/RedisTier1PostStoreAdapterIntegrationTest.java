package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.CachedPost;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisTier1PostStoreAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 개별 String 기반 게시글 캐시 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisTier1PostStoreAdapterIntegrationTest {

    @Autowired
    private RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;

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
    @DisplayName("정상 케이스 - MGET으로 캐시된 게시글 조회")
    void shouldReturnCachedPosts_WhenCacheExists() {
        // Given: 개별 String 캐시로 저장
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        PostSimpleDetail post1 = toSimpleDetail(testPostDetail1);
        PostSimpleDetail post2 = toSimpleDetail(testPostDetail2);

        redisTier1PostStoreAdapter.cachePost(type, post1);
        redisTier1PostStoreAdapter.cachePost(type, post2);

        // When: MGET으로 조회
        Map<Long, CachedPost> result = redisTier1PostStoreAdapter.getCachedPosts(type, List.of(1L, 2L));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L).getData().getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.get(2L).getData().getTitle()).isEqualTo("두 번째 게시글");
    }

    @Test
    @DisplayName("정상 케이스 - 캐시 미스 ID 필터링")
    void shouldFilterMissedIds_WhenSomePostsNotCached() {
        // Given: 일부만 캐시됨
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        PostSimpleDetail post1 = toSimpleDetail(testPostDetail1);
        redisTier1PostStoreAdapter.cachePost(type, post1);

        Map<Long, CachedPost> cachedPosts = redisTier1PostStoreAdapter.getCachedPosts(type, List.of(1L, 2L, 3L));

        // When: 캐시 미스 ID 필터링
        List<Long> missedIds = redisTier1PostStoreAdapter.filterMissedIds(List.of(1L, 2L, 3L), cachedPosts);

        // Then
        assertThat(missedIds).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("정상 케이스 - 개별 게시글 캐시 저장 및 TTL 확인")
    void shouldCachePost_WithCorrectTTL() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        PostSimpleDetail post = toSimpleDetail(testPostDetail1);

        // When: 개별 저장
        redisTier1PostStoreAdapter.cachePost(type, post);

        // Then: 캐시 키 존재 및 TTL 확인
        String key = RedisPostKeys.getSimplePostKey(type, post.getId());
        assertThat(redisTemplate.hasKey(key)).isTrue();

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L); // 5분 TTL
    }

    @Test
    @DisplayName("정상 케이스 - 여러 게시글 캐시 저장")
    void shouldCachePosts_WhenMultiplePostsProvided() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME;
        List<PostSimpleDetail> posts = List.of(
                toSimpleDetail(testPostDetail1),
                toSimpleDetail(testPostDetail2),
                toSimpleDetail(testPostDetail3)
        );

        // When: 여러 개 저장
        redisTier1PostStoreAdapter.cachePosts(type, posts);

        // Then: 모든 키 존재 확인
        for (PostSimpleDetail post : posts) {
            String key = RedisPostKeys.getSimplePostKey(type, post.getId());
            assertThat(redisTemplate.hasKey(key)).isTrue();
        }
    }

    @Test
    @DisplayName("정상 케이스 - 단일 캐시 삭제")
    void shouldRemovePostFromCache() {
        // Given: 모든 타입에 게시글 캐시 저장
        Long postId = 1L;
        PostSimpleDetail post = toSimpleDetail(testPostDetail1);

        for (PostCacheFlag type : PostCacheFlag.values()) {
            redisTier1PostStoreAdapter.cachePost(type, post);
        }

        // 저장 확인
        for (PostCacheFlag type : PostCacheFlag.values()) {
            String key = RedisPostKeys.getSimplePostKey(type, postId);
            assertThat(redisTemplate.hasKey(key)).isTrue();
        }

        // When: 삭제
        redisTier1PostStoreAdapter.removePostFromCache(postId);

        // Then: 모든 타입에서 삭제됨
        for (PostCacheFlag type : PostCacheFlag.values()) {
            String key = RedisPostKeys.getSimplePostKey(type, postId);
            assertThat(redisTemplate.hasKey(key)).isFalse();
        }
    }

    @Test
    @DisplayName("정상 케이스 - 특정 타입의 캐시만 삭제")
    void shouldRemovePostFromCache_SpecificType() {
        // Given: WEEKLY와 LEGEND에 게시글 캐시 저장
        Long postId = 1L;
        PostSimpleDetail post = toSimpleDetail(testPostDetail1);

        redisTier1PostStoreAdapter.cachePost(PostCacheFlag.WEEKLY, post);
        redisTier1PostStoreAdapter.cachePost(PostCacheFlag.LEGEND, post);

        // When: WEEKLY만 삭제
        redisTier1PostStoreAdapter.removePostFromCache(PostCacheFlag.WEEKLY, postId);

        // Then: WEEKLY는 삭제, LEGEND는 유지
        assertThat(redisTemplate.hasKey(RedisPostKeys.getSimplePostKey(PostCacheFlag.WEEKLY, postId))).isFalse();
        assertThat(redisTemplate.hasKey(RedisPostKeys.getSimplePostKey(PostCacheFlag.LEGEND, postId))).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 순서 유지된 리스트 변환")
    void shouldConvertToOrderedList() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME;
        PostSimpleDetail post1 = toSimpleDetail(testPostDetail1);
        PostSimpleDetail post2 = toSimpleDetail(testPostDetail2);
        PostSimpleDetail post3 = toSimpleDetail(testPostDetail3);

        redisTier1PostStoreAdapter.cachePosts(type, List.of(post1, post2, post3));

        Map<Long, CachedPost> cachedPosts = redisTier1PostStoreAdapter.getCachedPosts(type, List.of(1L, 2L, 3L));

        // When: 역순으로 정렬 요청
        List<Long> orderedIds = List.of(3L, 1L, 2L);
        List<PostSimpleDetail> result = redisTier1PostStoreAdapter.toOrderedList(orderedIds, cachedPosts);

        // Then: 요청된 순서대로 반환
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
        assertThat(result.get(2).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("경계값 - 빈 리스트로 MGET 조회 시 빈 Map 반환")
    void shouldReturnEmptyMap_WhenEmptyListProvided() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;

        // When
        Map<Long, CachedPost> result = redisTier1PostStoreAdapter.getCachedPosts(type, List.of());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - null 리스트로 MGET 조회 시 빈 Map 반환")
    void shouldReturnEmptyMap_WhenNullListProvided() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;

        // When
        Map<Long, CachedPost> result = redisTier1PostStoreAdapter.getCachedPosts(type, null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - Hash Tag가 포함된 키 생성 확인")
    void shouldGenerateKeyWithHashTag() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME;
        Long postId = 123L;

        // When
        String key = RedisPostKeys.getSimplePostKey(type, postId);

        // Then: Hash Tag 포함 확인
        assertThat(key).contains("{realtime}");
        assertThat(key).isEqualTo("post:{realtime}:simple:123");
    }

    @Test
    @DisplayName("정상 케이스 - 여러 키 생성 시 모두 같은 Hash Tag")
    void shouldGenerateKeysWithSameHashTag() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        List<Long> postIds = List.of(1L, 2L, 3L);

        // When
        List<String> keys = RedisPostKeys.getSimplePostKeys(type, postIds);

        // Then: 모든 키가 같은 Hash Tag 포함
        for (String key : keys) {
            assertThat(key).contains("{weekly}");
        }
    }
}
