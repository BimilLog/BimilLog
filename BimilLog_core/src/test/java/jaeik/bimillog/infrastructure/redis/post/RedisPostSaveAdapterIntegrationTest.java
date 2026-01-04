package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPostSaveAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 게시글 캐시 저장 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPostSaveAdapterIntegrationTest {

    @Autowired
    private RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;

    @Autowired
    private RedisDetailPostStoreAdapter redisDetailPostStoreAdapter;

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
    @DisplayName("정상 케이스 - 인기글 postId 영구 저장소 저장 (post:weekly:postids)")
    void shouldCachePostIdsOnly_WhenValidPostsProvided() {
        // Given
        List<Long> postIds = List.of(1L, 2L, 3L);
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String storageKey = RedisPostKeys.getPostIdsStorageKey(cacheType);  // postId 영구 저장소 (Sorted Set)

        // When
        redisTier2PostStoreAdapter.cachePostIdsOnly(cacheType, postIds);

        // Then: Sorted Set에 실제로 저장되었는지 확인
        Long size = redisTemplate.opsForZSet().size(storageKey);
        assertThat(size).isEqualTo(3);
        assertThat(redisTemplate.opsForZSet().score(storageKey, "1")).isNotNull();
        assertThat(redisTemplate.opsForZSet().score(storageKey, "2")).isNotNull();
        assertThat(redisTemplate.opsForZSet().score(storageKey, "3")).isNotNull();

        // TTL 확인 (1일 = 86400초)
        Long ttl = redisTemplate.getExpire(storageKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(86390L, 86400L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 상세 캐시 저장")
    void shouldCachePostDetail_WhenValidPostDetailProvided() {
        // Given
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(testPostDetail.getId());

        // When
        redisDetailPostStoreAdapter.cachePostDetail(testPostDetail);

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
    @DisplayName("경계값 - 빈 목록으로 postId 저장 시도")
    void shouldHandleEmptyList_WhenCachingPostIdsOnly() {
        // Given
        List<Long> emptyPostIds = List.of();
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String storageKey = RedisPostKeys.getPostIdsStorageKey(cacheType);

        // When: 빈 목록으로 저장 (아무 동작도 하지 않아야 함)
        redisTier2PostStoreAdapter.cachePostIdsOnly(cacheType, emptyPostIds);

        // Then: 저장소 키가 생성되지 않음
        assertThat(redisTemplate.hasKey(storageKey)).isFalse();
    }
}
