package jaeik.bimillog.adapter.out.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.infrastructure.adapter.out.redis.post.RedisPostDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.out.redis.post.RedisPostSaveAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>RedisPostDeleteAdapter 통합 테스트</h2>
 * <p>Redis TestContainers를 사용한 실제 Redis 환경에서의 테스트</p>
 * <p>게시글 캐시 삭제 어댑터의 핵심 기능을 검증합니다.</p>
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
    @DisplayName("정상 케이스 - 타입별 캐시 삭제 (목록 + 상세)")
    void shouldDeleteCache_WhenValidCacheTypeProvided() {
        // Given: 데이터 준비
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        List<Long> postIds = List.of(1L, 2L);

        // 목록 캐시와 상세 캐시 모두 저장
        redisPostSaveAdapter.cachePostIds(cacheType, postIds);
        redisPostSaveAdapter.cachePostDetail(testPostDetail);
        redisPostSaveAdapter.cachePostDetail(
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
        redisPostDeleteAdapter.deleteCache(cacheType, null);

        // Then: 목록과 상세 캐시 모두 삭제됨
        assertThat(redisTemplate.hasKey(listKey)).isFalse();
        assertThat(redisTemplate.hasKey(detailKey1)).isFalse();
        assertThat(redisTemplate.hasKey(detailKey2)).isFalse();
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
    @DisplayName("정상 케이스 - 특정 게시글의 모든 캐시 삭제")
    void shouldDeleteAllCachesOfPost_WhenPostIdProvided() {
        // Given: 여러 타입의 캐시에 게시글 저장
        Long postId = 1L;

        // WEEKLY와 LEGEND 목록에 추가
        redisPostSaveAdapter.cachePostIds(PostCacheFlag.WEEKLY, List.of(postId));
        redisPostSaveAdapter.cachePostIds(PostCacheFlag.LEGEND, List.of(postId));

        // 상세 캐시 추가
        redisPostSaveAdapter.cachePostDetail(testPostDetail);

        String weeklyKey = RedisTestHelper.RedisKeys.postList(PostCacheFlag.WEEKLY);
        String legendKey = RedisTestHelper.RedisKeys.postList(PostCacheFlag.LEGEND);
        String detailKey = RedisTestHelper.RedisKeys.postDetail(postId);

        // 저장 확인 (List에 포함되어 있는지)
        List<Object> weeklyPosts = redisTemplate.opsForList().range(weeklyKey, 0, -1);
        List<Object> legendPosts = redisTemplate.opsForList().range(legendKey, 0, -1);
        assertThat(weeklyPosts).contains(postId.toString());
        assertThat(legendPosts).contains(postId.toString());
        assertThat(redisTemplate.hasKey(detailKey)).isTrue();

        // When: 특정 게시글의 모든 캐시 삭제
        redisPostDeleteAdapter.deleteCache(null, postId);

        // Then: 상세 캐시와 모든 목록 캐시에서 제거됨
        assertThat(redisTemplate.hasKey(detailKey)).isFalse();
        weeklyPosts = redisTemplate.opsForList().range(weeklyKey, 0, -1);
        legendPosts = redisTemplate.opsForList().range(legendKey, 0, -1);
        assertThat(weeklyPosts).doesNotContain(postId.toString());
        assertThat(legendPosts).doesNotContain(postId.toString());
    }
}
