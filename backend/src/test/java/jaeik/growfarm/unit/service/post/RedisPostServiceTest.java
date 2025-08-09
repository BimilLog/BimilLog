package jaeik.growfarm.unit.service.post;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.post.PostScheduledService;
import jaeik.growfarm.service.redis.RedisPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>RedisPostService 단위 테스트</h2>
 * <p>
 * Redis 게시글 캐싱 관련 서비스의 메서드들을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
public class RedisPostServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private PostScheduledService postScheduledService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisPostService redisPostService;

    private List<SimplePostResDTO> mockPosts;
    private SimplePostResDTO mockPost;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        mockPost = SimplePostResDTO.builder()
                .postId(1L)
                .userId(1L)
                .userName("testUser")
                .title("Test Post")
                .commentCount(5)
                .likes(10)
                .views(100)
                .createdAt(Instant.now())
                .is_notice(false)
                .build();
        
        mockPosts = List.of(mockPost);
    }

    @Test
    @DisplayName("인기글 캐시 저장 테스트 - REALTIME")
    void testCachePopularPosts_Realtime() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;

        // When
        redisPostService.cachePopularPosts(type, mockPosts);

        // Then
        verify(valueOperations, times(1)).set(
                eq("popular:posts:realtime"),
                eq(mockPosts),
                eq(type.getTtl())
        );
    }

    @Test
    @DisplayName("인기글 캐시 저장 테스트 - WEEKLY")
    void testCachePopularPosts_Weekly() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.WEEKLY;

        // When
        redisPostService.cachePopularPosts(type, mockPosts);

        // Then
        verify(valueOperations, times(1)).set(
                eq("popular:posts:weekly"),
                eq(mockPosts),
                eq(type.getTtl())
        );
    }

    @Test
    @DisplayName("인기글 캐시 저장 테스트 - LEGEND")
    void testCachePopularPosts_Legend() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.LEGEND;

        // When
        redisPostService.cachePopularPosts(type, mockPosts);

        // Then
        verify(valueOperations, times(1)).set(
                eq("popular:posts:legend"),
                eq(mockPosts),
                eq(type.getTtl())
        );
    }

    @Test
    @DisplayName("인기글 캐시 저장 실패 테스트")
    void testCachePopularPosts_Failure() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(anyString(), any(), any());

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> redisPostService.cachePopularPosts(type, mockPosts));

        assertEquals(ErrorCode.REDIS_WRITE_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("인기글 캐시 조회 테스트 - 캐시 존재")
    void testGetCachedPopularPosts_CacheExists() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime")).thenReturn(true);
        when(valueOperations.get("popular:posts:realtime")).thenReturn(mockPosts);

        // When
        List<SimplePostResDTO> result = redisPostService.getCachedPopularPosts(type);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Post", result.get(0).getTitle());
        verify(postScheduledService, never()).updateRealtimePopularPosts();
    }

    @Test
    @DisplayName("인기글 캐시 조회 테스트 - 캐시 미존재 (REALTIME)")
    void testGetCachedPopularPosts_CacheMiss_Realtime() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime")).thenReturn(false);
        when(valueOperations.get("popular:posts:realtime")).thenReturn(mockPosts);

        // When
        List<SimplePostResDTO> result = redisPostService.getCachedPopularPosts(type);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(postScheduledService, times(1)).updateRealtimePopularPosts();
    }

    @Test
    @DisplayName("인기글 캐시 조회 테스트 - 캐시 미존재 (WEEKLY)")
    void testGetCachedPopularPosts_CacheMiss_Weekly() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.WEEKLY;
        when(redisTemplate.hasKey("popular:posts:weekly")).thenReturn(false);
        when(valueOperations.get("popular:posts:weekly")).thenReturn(mockPosts);

        // When
        List<SimplePostResDTO> result = redisPostService.getCachedPopularPosts(type);

        // Then
        assertNotNull(result);
        verify(postScheduledService, times(1)).updateWeeklyPopularPosts();
    }

    @Test
    @DisplayName("인기글 캐시 조회 테스트 - 캐시 미존재 (LEGEND)")
    void testGetCachedPopularPosts_CacheMiss_Legend() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.LEGEND;
        when(redisTemplate.hasKey("popular:posts:legend")).thenReturn(false);
        when(valueOperations.get("popular:posts:legend")).thenReturn(mockPosts);

        // When
        List<SimplePostResDTO> result = redisPostService.getCachedPopularPosts(type);

        // Then
        assertNotNull(result);
        verify(postScheduledService, times(1)).updateLegendPopularPosts();
    }

    @Test
    @DisplayName("인기글 캐시 조회 테스트 - 빈 캐시")
    void testGetCachedPopularPosts_EmptyCache() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime")).thenReturn(true);
        when(valueOperations.get("popular:posts:realtime")).thenReturn(null);

        // When
        List<SimplePostResDTO> result = redisPostService.getCachedPopularPosts(type);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("인기글 캐시 조회 실패 테스트")
    void testGetCachedPopularPosts_Failure() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime")).thenReturn(true);
        when(valueOperations.get("popular:posts:realtime"))
                .thenThrow(new RuntimeException("Redis connection error"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> redisPostService.getCachedPopularPosts(type));

        assertEquals(ErrorCode.REDIS_READ_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("인기글 캐시 삭제 테스트")
    void testDeletePopularPostsCache() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;

        // When
        redisPostService.deletePopularPostsCache(type);

        // Then
        verify(redisTemplate, times(1)).delete("popular:posts:realtime");
    }

    @Test
    @DisplayName("인기글 캐시 삭제 실패 테스트")
    void testDeletePopularPostsCache_Failure() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        doThrow(new RuntimeException("Redis connection error"))
                .when(redisTemplate).delete(anyString());

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> redisPostService.deletePopularPostsCache(type));

        assertEquals(ErrorCode.REDIS_DELETE_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("인기글 캐시 존재 확인 테스트 - 존재")
    void testHasPopularPostsCache_Exists() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime")).thenReturn(true);

        // When
        boolean result = redisPostService.hasPopularPostsCache(type);

        // Then
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey("popular:posts:realtime");
    }

    @Test
    @DisplayName("인기글 캐시 존재 확인 테스트 - 미존재")
    void testHasPopularPostsCache_NotExists() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime")).thenReturn(false);

        // When
        boolean result = redisPostService.hasPopularPostsCache(type);

        // Then
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey("popular:posts:realtime");
    }

    @Test
    @DisplayName("모든 인기글 캐시 삭제 테스트")
    void testDeleteAllPopularPostsCache() {
        // When
        redisPostService.deleteAllPopularPostsCache();

        // Then
        verify(redisTemplate, times(1)).delete("popular:posts:realtime");
        verify(redisTemplate, times(1)).delete("popular:posts:weekly");
        verify(redisTemplate, times(1)).delete("popular:posts:legend");
    }

    @Test
    @DisplayName("캐시 존재 확인 실패 테스트")
    void testHasPopularPostsCache_Failure() {
        // Given
        RedisPostService.CachePostType type = RedisPostService.CachePostType.REALTIME;
        when(redisTemplate.hasKey("popular:posts:realtime"))
                .thenThrow(new RuntimeException("Redis connection error"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class,
                () -> redisPostService.hasPopularPostsCache(type));

        assertEquals(ErrorCode.REDIS_READ_ERROR, exception.getErrorCode());
    }
}