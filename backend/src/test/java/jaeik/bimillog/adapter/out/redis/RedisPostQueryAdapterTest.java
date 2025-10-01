package jaeik.bimillog.adapter.out.redis;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.redis.RedisPostQueryAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

/**
 * <h2>RedisPostQueryAdapter 테스트</h2>
 * <p>Redis 캐시 조회 어댑터의 핵심 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Tag("test")
class RedisPostQueryAdapterTest extends BaseUnitTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisPostQueryAdapter redisPostQueryAdapter;

    private PostDetail testPostDetail;

    @BeforeEach
    void setUp() {
        testPostDetail = PostDetail.builder()
            .id(1L)
            .title("캐시된 게시글")
            .content("캐시된 내용")
            .viewCount(100)
            .likeCount(50)
            .commentCount(10)
            .isLiked(false)
            .postCacheFlag(PostCacheFlag.REALTIME)
            .createdAt(java.time.Instant.now())
            .memberId(1L)
            .memberName("testMember")
            .isNotice(false)
            .build();
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 상세 조회")
    void shouldReturnPostDetail_WhenCachedPostExists() {
        // Given
        Long postId = 1L;
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(RedisTestHelper.RedisKeys.postDetail(postId)))
            .willReturn(testPostDetail);

        // When
        PostDetail result = redisPostQueryAdapter.getCachedPostIfExists(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(postId);
        assertThat(result.title()).isEqualTo("캐시된 게시글");
        verify(valueOperations).get(RedisTestHelper.RedisKeys.postDetail(postId));
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 목록 조회")
    void shouldReturnPostList_WhenCachedListExists() {
        // Given
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        Set<Object> postIds = Set.of("1", "2", "3");

        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(zSetOperations.reverseRange(RedisTestHelper.RedisKeys.postList(cacheType), 0, -1))
            .willReturn(postIds);
        given(valueOperations.get(RedisTestHelper.RedisKeys.postDetail(1L))).willReturn(testPostDetail);
        given(valueOperations.get(RedisTestHelper.RedisKeys.postDetail(2L))).willReturn(testPostDetail);
        given(valueOperations.get(RedisTestHelper.RedisKeys.postDetail(3L))).willReturn(testPostDetail);

        // When
        List<PostSearchResult> result = redisPostQueryAdapter.getCachedPostList(cacheType);

        // Then
        assertThat(result).hasSize(3);
        verify(zSetOperations).reverseRange(RedisTestHelper.RedisKeys.postList(cacheType), 0, -1);
    }

    @Test
    @DisplayName("정상 케이스 - 캐시 존재 여부 확인")
    void shouldReturnTrue_WhenCacheKeyExists() {
        // Given
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        given(redisTemplate.hasKey(RedisTestHelper.RedisKeys.postList(cacheType)))
            .willReturn(true);

        // When
        boolean result = redisPostQueryAdapter.hasPopularPostsCache(cacheType);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(RedisTestHelper.RedisKeys.postList(cacheType));
    }

    @Test
    @DisplayName("정상 케이스 - 빈 캐시 목록 조회")
    void shouldReturnEmptyList_WhenNoCachedPostsExist() {
        // Given
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(RedisTestHelper.RedisKeys.postList(cacheType), 0, -1))
            .willReturn(Collections.emptySet());

        // When
        List<PostSearchResult> result = redisPostQueryAdapter.getCachedPostList(cacheType);

        // Then
        assertThat(result).isEmpty();
        verify(zSetOperations).reverseRange(RedisTestHelper.RedisKeys.postList(cacheType), 0, -1);
    }

    @Test
    @DisplayName("경계값 - 캐시되지 않은 게시글 조회 시 null 반환")
    void shouldReturnNull_WhenPostNotCached() {
        // Given
        Long nonExistentPostId = 999L;
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(RedisTestHelper.RedisKeys.postDetail(nonExistentPostId)))
            .willReturn(null);

        // When
        PostDetail result = redisPostQueryAdapter.getCachedPostIfExists(nonExistentPostId);

        // Then
        assertThat(result).isNull();
        verify(valueOperations).get(RedisTestHelper.RedisKeys.postDetail(nonExistentPostId));
    }

    @Test
    @DisplayName("예외 케이스 - Redis 조회 오류 시 PostCustomException 발생")
    void shouldThrowCustomException_WhenRedisReadError() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString()))
            .willThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertThatThrownBy(() -> redisPostQueryAdapter.getCachedPostIfExists(1L))
            .isInstanceOf(PostCustomException.class)
            .satisfies(ex -> {
                PostCustomException customEx = (PostCustomException) ex;
                assertThat(customEx.getPostErrorCode().getStatus())
                    .isEqualTo(PostErrorCode.REDIS_READ_ERROR.getStatus());
            });
    }
}