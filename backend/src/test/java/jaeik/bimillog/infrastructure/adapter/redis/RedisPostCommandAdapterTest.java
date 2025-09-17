package jaeik.bimillog.infrastructure.adapter.redis;

import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * <h2>RedisPostCommandAdapter 테스트</h2>
 * <p>Redis 캐시 명령 어댑터의 핵심 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class RedisPostCommandAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private RedisPostCommandAdapter redisPostCommandAdapter;

    private PostDetail testPostDetail;

    @BeforeEach
    void setUp() {
        redisPostCommandAdapter = new RedisPostCommandAdapter(redisTemplate, jpaQueryFactory);

        testPostDetail = PostDetail.builder()
            .id(1L)
            .title("테스트 게시글")
            .content("테스트 내용")
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
    @DisplayName("정상 케이스 - 게시글 목록과 상세 캐시 저장")
    void shouldCachePosts_WhenValidPostsProvided() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        
        List<PostDetail> postDetails = List.of(testPostDetail);
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When
        redisPostCommandAdapter.cachePostsWithDetails(cacheType, postDetails);

        // Then
        verify(zSetOperations).add(eq("cache:posts:realtime"), eq("1"), eq(50.0));
        verify(redisTemplate).expire(eq("cache:posts:realtime"), eq(Duration.ofMinutes(30)));
        verify(valueOperations).set(eq("cache:post:1"), eq(testPostDetail), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - 인기 게시글 플래그 적용")
    void shouldApplyPopularFlag_WhenValidPostIdsProvided() {
        // Given
        List<Long> postIds = List.of(1L, 2L);
        PostCacheFlag cacheFlag = PostCacheFlag.REALTIME;

        JPAUpdateClause updateClause = mock(JPAUpdateClause.class);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.set(any(Path.class), any(Object.class))).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(2L);

        // When
        redisPostCommandAdapter.applyPopularFlag(postIds, cacheFlag);

        // Then
        verify(jpaQueryFactory).update(any());
        verify(updateClause).set(any(Path.class), eq(cacheFlag));
        verify(updateClause).execute();
    }

    @Test
    @DisplayName("정상 케이스 - 캐시 삭제")
    void shouldDeleteCache_WhenValidCacheTypeProvided() {
        // Given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When
        redisPostCommandAdapter.deleteCache(cacheType, null);

        // Then
        verify(redisTemplate).delete("cache:posts:realtime");
    }

    @Test
    @DisplayName("예외 케이스 - Redis 쓰기 오류 시 PostCustomException 발생")
    void shouldThrowCustomException_WhenRedisWriteError() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), any(), any(Duration.class));

        List<PostDetail> postDetails = List.of(testPostDetail);

        // When & Then
        assertThatThrownBy(() -> redisPostCommandAdapter.cachePostsWithDetails(PostCacheFlag.REALTIME, postDetails))
            .isInstanceOf(PostCustomException.class)
            .satisfies(ex -> {
                PostCustomException customEx = (PostCustomException) ex;
                assertThat(customEx.getPostErrorCode().getStatus())
                    .isEqualTo(PostErrorCode.REDIS_WRITE_ERROR.getStatus());
            });
    }
}