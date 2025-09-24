package jaeik.bimillog.infrastructure.adapter.out.redis;

import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.verify;

/**
 * <h2>RedisPostCommandAdapter 테스트</h2>
 * <p>Redis 캐시 명령 어댑터의 핵심 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
class RedisPostCommandAdapterTest extends BaseUnitTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisPostCommandAdapter redisPostCommandAdapter;

    private PostDetail testPostDetail;

    @BeforeEach
    void setUp() {
        RedisTestHelper.setupRedisTemplateMocks(redisTemplate, valueOperations, zSetOperations);
        testPostDetail = RedisTestHelper.defaultPostDetail();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 목록과 상세 캐시 저장")
    void shouldCachePosts_WhenValidPostsProvided() {
        // Given
        List<PostDetail> postDetails = List.of(testPostDetail);
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When
        redisPostCommandAdapter.cachePostsWithDetails(cacheType, postDetails);

        // Then
        verify(zSetOperations).add(eq(RedisTestHelper.RedisKeys.postList(cacheType)), eq("1"), eq(50.0));
        verify(redisTemplate).expire(eq(RedisTestHelper.RedisKeys.postList(cacheType)), eq(Duration.ofMinutes(30)));
        verify(valueOperations).set(eq(RedisTestHelper.RedisKeys.postDetail(1L)), eq(testPostDetail), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("정상 케이스 - 인기 게시글 플래그 적용")
    void shouldApplyPopularFlag_WhenValidPostIdsProvided() {
        // Given
        List<Long> postIds = List.of(1L, 2L);
        PostCacheFlag cacheFlag = PostCacheFlag.REALTIME;
        
        JPAUpdateClause updateClause = RedisTestHelper.setupJpaUpdateClauseMock(jpaQueryFactory, 2L);

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
        PostCacheFlag cacheType = PostCacheFlag.REALTIME;
        
        // When
        redisPostCommandAdapter.deleteCache(cacheType, null);

        // Then
        verify(redisTemplate).delete(RedisTestHelper.RedisKeys.postList(cacheType));
    }

    @Test
    @DisplayName("예외 케이스 - Redis 쓰기 오류 시 PostCustomException 발생")
    void shouldThrowCustomException_WhenRedisWriteError() {
        // Given
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