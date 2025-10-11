package jaeik.bimillog.adapter.out.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.infrastructure.adapter.out.redis.post.RedisPostDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.out.redis.post.RedisPostKeys;
import jaeik.bimillog.infrastructure.adapter.out.redis.post.RedisPostSaveAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPostDeleteAdapter 통합 테스트</h2>
 * <p>로컬 Redis 환경에서의 테스트</p>
 * <p>게시글 캐시 삭제 어댑터의 핵심 기능을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisPostDeleteAdapterIntegrationTest {

    @Autowired
    private RedisPostDeleteAdapter redisPostDeleteAdapter;

    @Autowired
    private RedisPostSaveAdapter redisPostSaveAdapter;

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
    @DisplayName("정상 케이스 - 단일 게시글 캐시 삭제")
    void shouldDeleteSinglePostCache_WhenPostIdProvided() {
        // Given: 게시글 상세 캐시 저장
        redisPostSaveAdapter.cachePostDetail(testPostDetail);
        String cacheKey = RedisTestHelper.RedisKeys.postDetail(testPostDetail.getId());
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // When: 단일 게시글 캐시 삭제
        redisPostDeleteAdapter.deleteSinglePostCache(testPostDetail.getId());

        // Then: 캐시가 삭제됨
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기글 점수 저장소에서 게시글 제거")
    void shouldRemovePostIdFromRealtimeScore() {
        // Given: post:realtime:score에 postId 추가
        Long postId = 1L;
        String scoreKey = RedisPostKeys.REALTIME_POPULAR_SCORE_KEY;

        redisTemplate.opsForZSet().add(scoreKey, postId.toString(), 100.0);

        // 저장 확인
        Double score = redisTemplate.opsForZSet().score(scoreKey, postId.toString());
        assertThat(score).isEqualTo(100.0);

        // When: removePostIdFromRealtimeScore() 호출
        redisPostDeleteAdapter.removePostIdFromRealtimeScore(postId);

        // Then: Sorted Set에서 제거됨 확인
        Double scoreAfter = redisTemplate.opsForZSet().score(scoreKey, postId.toString());
        assertThat(scoreAfter).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 목록 캐시 전체 삭제")
    void shouldClearPostListCache() {
        // Given: post:weekly:list Hash에 여러 게시글 추가
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();

        redisTemplate.opsForHash().put(hashKey, "1", testPostDetail);
        redisTemplate.opsForHash().put(hashKey, "2", testPostDetail);

        // 저장 확인
        assertThat(redisTemplate.hasKey(hashKey)).isTrue();
        assertThat(redisTemplate.opsForHash().size(hashKey)).isEqualTo(2);

        // When: clearPostListCache() 호출
        redisPostDeleteAdapter.clearPostListCache(type);

        // Then: Hash 전체가 삭제됨 확인
        assertThat(redisTemplate.hasKey(hashKey)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 목록 캐시에서 단일 게시글 제거 (모든 Hash 필드 삭제)")
    void shouldRemovePostFromListCache() {
        // Given: 모든 타입의 Hash에 게시글 추가
        Long postId = 1L;

        // 모든 캐시 타입에 게시글 추가
        String realtimeKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.REALTIME).key();
        String weeklyKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.WEEKLY).key();
        String legendKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.LEGEND).key();
        String noticeKey = RedisPostKeys.CACHE_METADATA_MAP.get(PostCacheFlag.NOTICE).key();

        redisTemplate.opsForHash().put(realtimeKey, postId.toString(), testPostDetail);
        redisTemplate.opsForHash().put(weeklyKey, postId.toString(), testPostDetail);
        redisTemplate.opsForHash().put(legendKey, postId.toString(), testPostDetail);
        redisTemplate.opsForHash().put(noticeKey, postId.toString(), testPostDetail);

        // 저장 확인
        assertThat(redisTemplate.opsForHash().hasKey(realtimeKey, postId.toString())).isTrue();
        assertThat(redisTemplate.opsForHash().hasKey(weeklyKey, postId.toString())).isTrue();

        // When: removePostFromListCache() 호출 (모든 타입에서 제거)
        redisPostDeleteAdapter.removePostFromListCache(postId);

        // Then: 모든 Hash에서 필드가 삭제됨 확인
        assertThat(redisTemplate.opsForHash().hasKey(realtimeKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(weeklyKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(legendKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForHash().hasKey(noticeKey, postId.toString())).isFalse();
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
        redisPostDeleteAdapter.removePostIdFromStorage(postId);

        // Then: 모든 저장소에서 제거됨 확인
        assertThat(redisTemplate.opsForSet().isMember(noticeStorageKey, postId.toString())).isFalse();
        assertThat(redisTemplate.opsForZSet().score(weeklyStorageKey, postId.toString())).isNull();
        assertThat(redisTemplate.opsForZSet().score(legendStorageKey, postId.toString())).isNull();
    }
}
