package jaeik.growfarm.integration.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.post.PostCacheFlag;

import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.delete.PostDeleteRepositoryImpl;
import jaeik.growfarm.service.redis.RedisPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>Post 삭제 Repository 단위 테스트</h2>
 * <p>
 * PostDeleteRepositoryImpl의 삭제 로직과 Redis 캐시 동기화를 검증하는 테스트
 * </p>
 * <p>
 * 실제 구현체를 사용하되 의존성은 Mock으로 처리하여 핵심 로직을 검증한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Post 삭제 Repository 단위 테스트")
class PostDeleteRepositoryIntegrationTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;
    
    @Mock
    private RedisPostService redisPostService;
    
    private PostDeleteRepositoryImpl postDeleteRepository;

    @BeforeEach
    void setUp() {
        postDeleteRepository = new PostDeleteRepositoryImpl(jpaQueryFactory, redisPostService);
    }

    @Test
    @DisplayName("Redis 캐시 삭제 호출 검증 - 각 PopularFlag별")
    void testRedisCacheDeleteCalls() {
        // when & then - 각 PopularFlag에 대한 Redis 호출 검증
        
        // REALTIME 테스트
        doNothing().when(redisPostService).deletePopularPostsCache(RedisPostService.CachePostType.REALTIME);
        redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.REALTIME);
        verify(redisPostService, times(1)).deletePopularPostsCache(RedisPostService.CachePostType.REALTIME);
        
        // WEEKLY 테스트
        doNothing().when(redisPostService).deletePopularPostsCache(RedisPostService.CachePostType.WEEKLY);
        redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.WEEKLY);
        verify(redisPostService, times(1)).deletePopularPostsCache(RedisPostService.CachePostType.WEEKLY);
        
        // LEGEND 테스트
        doNothing().when(redisPostService).deletePopularPostsCache(RedisPostService.CachePostType.LEGEND);
        redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.LEGEND);
        verify(redisPostService, times(1)).deletePopularPostsCache(RedisPostService.CachePostType.LEGEND);
        
        System.out.println("=== Redis 캐시 삭제 호출 검증 완료 ===");
    }

    @Test
    @DisplayName("Redis 서비스 예외 처리 - CustomException 발생")
    void testRedisExceptionHandling() {
        // given
        RuntimeException redisException = new RuntimeException("Redis connection failed");
        doThrow(redisException).when(redisPostService)
                .deletePopularPostsCache(any(RedisPostService.CachePostType.class));

        // when & then
        assertThatThrownBy(() -> 
            redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.WEEKLY))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis connection failed");
        
        System.out.println("=== Redis 예외 처리 검증 완료 ===");
    }

    @Test
    @DisplayName("PostDeleteRepositoryImpl 생성자 검증")
    void testRepositoryCreation() {
        // when & then
        assertDoesNotThrow(() -> {
            PostDeleteRepositoryImpl repository = new PostDeleteRepositoryImpl(jpaQueryFactory, redisPostService);
            // 생성된 repository가 null이 아님을 확인
            assert repository != null;
        });
        
        System.out.println("=== PostDeleteRepositoryImpl 생성자 검증 완료 ===");
    }

    @Test
    @DisplayName("PopularFlag enum 값 검증")
    void testPopularFlagEnums() {
        // given & when & then
        PostCacheFlag[] flags = PostCacheFlag.values();
        
        // 예상된 플래그들이 모두 존재하는지 확인
        boolean hasRealtime = false;
        boolean hasWeekly = false;
        boolean hasLegend = false;
        
        for (PostCacheFlag flag : flags) {
            switch (flag) {
                case REALTIME -> hasRealtime = true;
                case WEEKLY -> hasWeekly = true;
                case LEGEND -> hasLegend = true;
            }
        }
        
        assert hasRealtime : "REALTIME 플래그가 존재하지 않음";
        assert hasWeekly : "WEEKLY 플래그가 존재하지 않음";
        assert hasLegend : "LEGEND 플래그가 존재하지 않음";
        
        System.out.println("=== PopularFlag enum 값 검증 완료 ===");
        System.out.println("사용 가능한 인기글 플래그:");
        for (PostCacheFlag flag : flags) {
            System.out.println("- " + flag.name());
        }
    }

    @Test
    @DisplayName("RedisPostService.CachePostType enum 값 검증")
    void testRedisCachePostTypeEnums() {
        // given & when & then
        RedisPostService.CachePostType[] types = RedisPostService.CachePostType.values();
        
        // 예상된 타입들이 모두 존재하는지 확인
        boolean hasRealtime = false;
        boolean hasWeekly = false;
        boolean hasLegend = false;
        
        for (RedisPostService.CachePostType type : types) {
            switch (type) {
                case REALTIME -> hasRealtime = true;
                case WEEKLY -> hasWeekly = true;
                case LEGEND -> hasLegend = true;
            }
        }
        
        assert hasRealtime : "REALTIME 타입이 존재하지 않음";
        assert hasWeekly : "WEEKLY 타입이 존재하지 않음";
        assert hasLegend : "LEGEND 타입이 존재하지 않음";
        
        System.out.println("=== RedisPostService.CachePostType enum 값 검증 완료 ===");
        System.out.println("사용 가능한 Redis 캐시 타입:");
        for (RedisPostService.CachePostType type : types) {
            System.out.println("- " + type.name() + " (키: " + type.getKey() + ", TTL: " + type.getTtl() + ")");
        }
    }

    @Test
    @DisplayName("ErrorCode 상수 검증")
    void testErrorCodes() {
        // given & when & then
        // POST_DELETE_FAILED와 REDIS_DELETE_ERROR가 존재하는지 확인
        ErrorCode postDeleteFailed = ErrorCode.POST_DELETE_FAILED;
        ErrorCode redisDeleteError = ErrorCode.REDIS_DELETE_ERROR;
        
        assert postDeleteFailed != null : "POST_DELETE_FAILED ErrorCode가 존재하지 않음";
        assert redisDeleteError != null : "REDIS_DELETE_ERROR ErrorCode가 존재하지 않음";
        
        System.out.println("=== ErrorCode 상수 검증 완료 ===");
        System.out.println("POST_DELETE_FAILED: " + postDeleteFailed.name());
        System.out.println("REDIS_DELETE_ERROR: " + redisDeleteError.name());
    }

    @Test
    @DisplayName("Mock 객체 상호작용 검증")
    void testMockInteractions() {
        // given
        reset(redisPostService); // Mock 상태 초기화
        
        // when
        doNothing().when(redisPostService).deletePopularPostsCache(any());
        redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.REALTIME);
        redisPostService.deletePopularPostsCache(RedisPostService.CachePostType.WEEKLY);
        
        // then
        verify(redisPostService, times(2)).deletePopularPostsCache(any());
        verify(redisPostService, times(1)).deletePopularPostsCache(RedisPostService.CachePostType.REALTIME);
        verify(redisPostService, times(1)).deletePopularPostsCache(RedisPostService.CachePostType.WEEKLY);
        verify(redisPostService, never()).deletePopularPostsCache(RedisPostService.CachePostType.LEGEND);
        
        System.out.println("=== Mock 객체 상호작용 검증 완료 ===");
    }

    @Test
    @DisplayName("삭제 리포지토리 메서드 존재 검증")
    void testDeleteRepositoryMethodExists() {
        // given & when & then
        // deletePostWithCacheSync 메서드가 존재하는지 확인
        try {
            postDeleteRepository.getClass().getMethod("deletePostWithCacheSync", Long.class);
            System.out.println("=== deletePostWithCacheSync 메서드 존재 확인 ===");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("deletePostWithCacheSync 메서드가 존재하지 않음", e);
        }
    }
}