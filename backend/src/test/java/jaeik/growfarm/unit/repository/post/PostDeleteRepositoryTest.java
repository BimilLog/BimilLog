package jaeik.growfarm.unit.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.delete.PostDeleteRepositoryImpl;
import jaeik.growfarm.service.redis.RedisPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>PostDeleteRepository 단위 테스트</h2>
 * <p>
 * 게시글 삭제 관련 Repository의 메서드들을 테스트합니다.
 * </p>
 * @version 1.1.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
public class PostDeleteRepositoryTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private RedisPostService redisPostService;

    @InjectMocks
    private PostDeleteRepositoryImpl postDeleteRepository;

    @BeforeEach
    void setUp() {
        // Mock JPAQueryFactory chain
        when(jpaQueryFactory.select(any())).thenReturn(jpaQueryFactory);
        when(jpaQueryFactory.from(any(QPost.class))).thenReturn(jpaQueryFactory);
        when(jpaQueryFactory.where(any())).thenReturn(jpaQueryFactory);
        when(jpaQueryFactory.delete(any(QPost.class))).thenReturn(jpaQueryFactory);
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 일반 게시글")
    void testDeletePostWithCacheSync_NormalPost() {
        // Given
        Long postId = 1L;
        when(jpaQueryFactory.fetchOne()).thenReturn(null); // popularFlag is null
        when(jpaQueryFactory.execute()).thenReturn(1L); // 1개 삭제됨

        // When
        postDeleteRepository.deletePostWithCacheSync(postId);

        // Then
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(jpaQueryFactory, times(1)).execute();
        verify(redisPostService, never()).deletePopularPostsCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 실시간 인기글")
    void testDeletePostWithCacheSync_RealtimePopular() {
        // Given
        Long postId = 1L;
        when(jpaQueryFactory.fetchOne()).thenReturn(PopularFlag.REALTIME);
        when(jpaQueryFactory.execute()).thenReturn(1L);

        // When
        postDeleteRepository.deletePostWithCacheSync(postId);

        // Then
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(jpaQueryFactory, times(1)).execute();
        verify(redisPostService, times(1))
                .deletePopularPostsCache(RedisPostService.PopularPostType.REALTIME);
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 주간 인기글")
    void testDeletePostWithCacheSync_WeeklyPopular() {
        // Given
        Long postId = 1L;
        when(jpaQueryFactory.fetchOne()).thenReturn(PopularFlag.WEEKLY);
        when(jpaQueryFactory.execute()).thenReturn(1L);

        // When
        postDeleteRepository.deletePostWithCacheSync(postId);

        // Then
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(jpaQueryFactory, times(1)).execute();
        verify(redisPostService, times(1))
                .deletePopularPostsCache(RedisPostService.PopularPostType.WEEKLY);
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 레전드 게시글")
    void testDeletePostWithCacheSync_LegendPopular() {
        // Given
        Long postId = 1L;
        when(jpaQueryFactory.fetchOne()).thenReturn(PopularFlag.LEGEND);
        when(jpaQueryFactory.execute()).thenReturn(1L);

        // When
        postDeleteRepository.deletePostWithCacheSync(postId);

        // Then
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(jpaQueryFactory, times(1)).execute();
        verify(redisPostService, times(1))
                .deletePopularPostsCache(RedisPostService.PopularPostType.LEGEND);
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - 존재하지 않는 게시글")
    void testDeletePostWithCacheSync_PostNotFound() {
        // Given
        Long postId = 999L;
        when(jpaQueryFactory.fetchOne()).thenReturn(null);
        when(jpaQueryFactory.execute()).thenReturn(0L); // 삭제된 게시글 없음

        // When
        postDeleteRepository.deletePostWithCacheSync(postId);

        // Then
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(jpaQueryFactory, times(1)).execute();
        verify(redisPostService, never()).deletePopularPostsCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - Redis 캐시 삭제 오류")
    void testDeletePostWithCacheSync_RedisError() {
        // Given
        Long postId = 1L;
        when(jpaQueryFactory.fetchOne()).thenReturn(PopularFlag.REALTIME);
        when(jpaQueryFactory.execute()).thenReturn(1L);
        doThrow(new RuntimeException("Redis connection error"))
                .when(redisPostService).deletePopularPostsCache(any());

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> postDeleteRepository.deletePostWithCacheSync(postId));

        assertEquals(ErrorCode.REDIS_DELETE_ERROR, exception.getErrorCode());
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(jpaQueryFactory, times(1)).execute();
        verify(redisPostService, times(1))
                .deletePopularPostsCache(RedisPostService.PopularPostType.REALTIME);
    }

    @Test
    @DisplayName("게시글 삭제 테스트 - DB 쿼리 오류")
    void testDeletePostWithCacheSync_DatabaseError() {
        // Given
        Long postId = 1L;
        when(jpaQueryFactory.fetchOne()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> postDeleteRepository.deletePostWithCacheSync(postId));

        assertEquals(ErrorCode.POST_DELETE_FAILED, exception.getErrorCode());
        verify(jpaQueryFactory, times(1)).fetchOne();
        verify(redisPostService, never()).deletePopularPostsCache(any());
    }
}