package jaeik.bimillog.infrastructure.redis.post;

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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisDetailPostStoreAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 게시글 상세 캐시 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisDetailPostStoreAdapterIntegrationTest {

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
    @DisplayName("정상 케이스 - 캐시된 게시글 상세 조회")
    void shouldReturnPostDetail_WhenCachedPostExists() {
        // Given: RedisTemplate로 직접 저장
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(1L);
        redisTemplate.opsForValue().set(cacheKey, testPostDetail, Duration.ofMinutes(5));

        // When: 상세 캐시 조회
        PostDetail result = redisDetailPostStoreAdapter.getCachedPostIfExists(1L);

        // Then: 저장한 데이터와 동일한 데이터 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("캐시된 게시글");
        assertThat(result.getContent()).isEqualTo("캐시된 내용");
        assertThat(result.getViewCount()).isEqualTo(100);
        assertThat(result.getLikeCount()).isEqualTo(50);
        assertThat(result.getCommentCount()).isEqualTo(10);
        assertThat(result.getMemberName()).isEqualTo("testMember");
    }

    @Test
    @DisplayName("경계값 - 캐시되지 않은 게시글 조회 시 null 반환")
    void shouldReturnNull_WhenPostNotCached() {
        // Given: 캐시되지 않은 게시글 ID
        Long nonExistentPostId = 999L;

        // When: 조회 시도
        PostDetail result = redisDetailPostStoreAdapter.getCachedPostIfExists(nonExistentPostId);

        // Then: null 반환
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 상세 캐시 저장")
    void shouldSaveCachePostDetail_WhenValidPostProvided() {
        // Given
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(testPostDetail.getId());

        // When: 상세 캐시 저장
        redisDetailPostStoreAdapter.saveCachePost(testPostDetail);

        // Then: 캐시 키가 존재하는지 확인
        Boolean keyExists = redisTemplate.hasKey(cacheKey);
        assertThat(keyExists).isTrue();

        // TTL 확인 (5분)
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L);

        // RedisTemplate로 직접 조회하여 검증
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
    void shouldDeleteIdProvidedPost() {
        // Given: 게시글 상세 캐시 저장
        redisDetailPostStoreAdapter.saveCachePost(testPostDetail);
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(testPostDetail.getId());
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // When: 단일 게시글 캐시 삭제
        redisDetailPostStoreAdapter.deleteCachePost(testPostDetail.getId());

        // Then: 캐시가 삭제됨
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }
}
