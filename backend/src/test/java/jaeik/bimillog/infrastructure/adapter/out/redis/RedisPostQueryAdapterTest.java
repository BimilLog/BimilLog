package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.redis.RedisPostQueryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
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
@ExtendWith(MockitoExtension.class)
class RedisPostQueryAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private RedisPostQueryAdapter redisPostQueryAdapter;

    private PostDetail testPostDetail;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        
        redisPostQueryAdapter = new RedisPostQueryAdapter(redisTemplate);

        testPostDetail = PostDetail.builder()
            .id(1L)
            .title("캐시된 게시글")
            .content("캐시된 내용")
            .viewCount(100)
            .likeCount(50)
            .postCacheFlag(PostCacheFlag.REALTIME)
            .createdAt(Instant.now())
            .userId(1L)
            .userName("testUser")
            .commentCount(10)
            .isNotice(false)
            .isLiked(false)
            .build();
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 상세 조회")
    void shouldReturnPostDetail_WhenCachedPostExists() {
        // Given
        given(valueOperations.get("cache:post:1"))
            .willReturn(testPostDetail);

        // When
        PostDetail result = redisPostQueryAdapter.getCachedPostIfExists(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("캐시된 게시글");
        verify(valueOperations).get("cache:post:1");
    }

    @Test
    @DisplayName("정상 케이스 - 캐시된 게시글 목록 조회")
    void shouldReturnPostList_WhenCachedListExists() {
        // Given
        Set<Object> postIds = Set.of("1", "2", "3");
        given(zSetOperations.reverseRange("cache:posts:realtime", 0, -1))
            .willReturn(postIds);
        given(valueOperations.get("cache:post:1")).willReturn(testPostDetail);
        given(valueOperations.get("cache:post:2")).willReturn(testPostDetail);
        given(valueOperations.get("cache:post:3")).willReturn(testPostDetail);

        // When
        List<PostSearchResult> result = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);

        // Then
        assertThat(result).hasSize(3);
        verify(zSetOperations).reverseRange("cache:posts:realtime", 0, -1);
    }

    @Test
    @DisplayName("정상 케이스 - 캐시 존재 여부 확인")
    void shouldReturnTrue_WhenCacheKeyExists() {
        // Given
        given(redisTemplate.hasKey("cache:posts:realtime"))
            .willReturn(true);

        // When
        boolean result = redisPostQueryAdapter.hasPopularPostsCache(PostCacheFlag.REALTIME);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey("cache:posts:realtime");
    }

    @Test
    @DisplayName("정상 케이스 - 빈 캐시 목록 조회")
    void shouldReturnEmptyList_WhenNoCachedPostsExist() {
        // Given
        given(zSetOperations.reverseRange("cache:posts:realtime", 0, -1))
            .willReturn(Collections.emptySet());

        // When
        List<PostSearchResult> result = redisPostQueryAdapter.getCachedPostList(PostCacheFlag.REALTIME);

        // Then
        assertThat(result).isEmpty();
        verify(zSetOperations).reverseRange("cache:posts:realtime", 0, -1);
    }

    @Test
    @DisplayName("경계값 - 캐시되지 않은 게시글 조회 시 null 반환")
    void shouldReturnNull_WhenPostNotCached() {
        // Given
        given(valueOperations.get("cache:post:999"))
            .willReturn(null);

        // When
        PostDetail result = redisPostQueryAdapter.getCachedPostIfExists(999L);

        // Then
        assertThat(result).isNull();
        verify(valueOperations).get("cache:post:999");
    }

    @Test
    @DisplayName("예외 케이스 - Redis 조회 오류 시 PostCustomException 발생")
    void shouldThrowCustomException_WhenRedisReadError() {
        // Given
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