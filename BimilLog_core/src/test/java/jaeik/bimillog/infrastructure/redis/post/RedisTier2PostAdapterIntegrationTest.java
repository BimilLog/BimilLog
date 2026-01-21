package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisTier2PostAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서 게시글 ID 목록 영구 저장소 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisTier2PostAdapterIntegrationTest {

    @Autowired
    private RedisTier2PostAdapter redisTier2PostAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("정상 케이스 - 저장된 게시글 ID 목록 조회 (WEEKLY)")
    void shouldGetStoredPostIds_WhenIdsExist() {
        // Given: Sorted Set에 postIds 저장
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        String postIdsKey = RedisPostKeys.getPostIdsStorageKey(type);

        redisTemplate.opsForZSet().add(postIdsKey, 1L, 1.0);
        redisTemplate.opsForZSet().add(postIdsKey, 2L, 2.0);
        redisTemplate.opsForZSet().add(postIdsKey, 3L, 3.0);

        // When: ID 목록 조회
        List<Long> result = redisTier2PostAdapter.getAllPostId(type);

        // Then: 순서대로 반환
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("경계값 - 저장된 ID가 없는 경우 빈 리스트 반환")
    void shouldReturnEmptyList_WhenNoIdsStored() {
        // Given: 저장소 없음
        PostCacheFlag type = PostCacheFlag.WEEKLY;

        // When: ID 목록 조회
        List<Long> result = redisTier2PostAdapter.getAllPostId(type);

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 인기글 postId 영구 저장소 저장 (post:weekly:postids)")
    void shouldCachePostIdsOnly_WhenValidPostsProvided() {
        // Given
        List<Long> postIds = List.of(1L, 2L, 3L);
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String storageKey = RedisPostKeys.getPostIdsStorageKey(cacheType);  // postId 영구 저장소 (Sorted Set)

        // When
        redisTier2PostAdapter.cachePostIdsOnly(cacheType, postIds);

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
    @DisplayName("경계값 - 빈 목록으로 postId 저장 시도")
    void shouldHandleEmptyList_WhenCachingPostIdsOnly() {
        // Given
        List<Long> emptyPostIds = List.of();
        PostCacheFlag cacheType = PostCacheFlag.WEEKLY;
        String storageKey = RedisPostKeys.getPostIdsStorageKey(cacheType);

        // When: 빈 목록으로 저장 (아무 동작도 하지 않아야 함)
        redisTier2PostAdapter.cachePostIdsOnly(cacheType, emptyPostIds);

        // Then: 저장소 키가 생성되지 않음
        assertThat(redisTemplate.hasKey(storageKey)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 단일 게시글 ID 추가 (NOTICE)")
    void shouldAddPostIdToStorage_WhenPostIdProvided() {
        // Given
        PostCacheFlag type = PostCacheFlag.NOTICE;
        Long postId = 10L;
        String postIdsKey = RedisPostKeys.getPostIdsStorageKey(type);

        // When: 단일 ID 추가
        redisTier2PostAdapter.addPostIdToStorage(type, postId);

        // Then: Set에 추가 확인 (NOTICE는 Set 사용)
        assertThat(redisTemplate.opsForSet().isMember(postIdsKey, "10")).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - postIds 저장소에서 게시글 ID 제거 (모든 타입)")
    void shouldRemovePostIdFromStorage() {
        // Given: 모든 저장소에 postId 추가
        Long postId = 1L;

        String noticeStorageKey = RedisPostKeys.getPostIdsStorageKey(PostCacheFlag.NOTICE);
        String weeklyStorageKey = RedisPostKeys.getPostIdsStorageKey(PostCacheFlag.WEEKLY);
        String legendStorageKey = RedisPostKeys.getPostIdsStorageKey(PostCacheFlag.LEGEND);

        // NOTICE: Set
        redisTemplate.opsForSet().add(noticeStorageKey, postId.toString());
        // WEEKLY, LEGEND: Sorted Set
        redisTemplate.opsForZSet().add(weeklyStorageKey, postId.toString(), 100.0);
        redisTemplate.opsForZSet().add(legendStorageKey, postId.toString(), 200.0);

        // 저장 확인
        assertThat(redisTemplate.opsForSet().isMember(noticeStorageKey, postId.toString())).isTrue();
        assertThat(redisTemplate.opsForZSet().score(weeklyStorageKey, postId.toString())).isNotNull();
        assertThat(redisTemplate.opsForZSet().score(legendStorageKey, postId.toString())).isNotNull();

        // When: removePostIdFromStorage() 호출 (REALTIME 제외한 모든 타입에서 제거)
        redisTier2PostAdapter.removePostIdFromStorage(postId);

        // Then: 모든 저장소에서 제거됨 확인
        assertThat(redisTemplate.opsForSet().isMember(noticeStorageKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForZSet().score(weeklyStorageKey, postId.toString())).isNull();
        assertThat(redisTemplate.opsForZSet().score(legendStorageKey, postId.toString())).isNull();
    }
}
