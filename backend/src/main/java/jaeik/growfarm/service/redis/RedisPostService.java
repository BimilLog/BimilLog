package jaeik.growfarm.service.redis;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.post.PostService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * <h2>Redis 게시글 서비스</h2>
 * <p>
 * Redis를 사용하여 다양한 유형의 인기 게시글 목록을 캐싱하고 관리하는 서비스
 * </p>
 * <p>실시간, 주간, 레전드 인기 게시글 목록을 하나의 서비스에서 관리한다.</p>
 * <p>
 * Redis에 캐싱하고, 조회, 삭제 등의 작업을 수행한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RedisPostService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostService postService;


    /**
     * <h3>인기글 유형</h3>
     * <p>
     * Redis에 저장되는 인기글 목록의 유형을 정의한다.
     * </p>
     * <p>
     * 각 유형은 Redis 키와 TTL(유효 기간)을 포함한다.
     * </p>
     * @author Jaeik
     * @version 1.0.0
     */
    @Getter
    public enum PopularPostType {
        REALTIME("popular:posts:realtime", Duration.ofMinutes(30)),
        WEEKLY("popular:posts:weekly", Duration.ofDays(1)),
        LEGEND("popular:posts:legend", Duration.ofDays(1));

        private final String key;
        private final Duration ttl;

        PopularPostType(String key, Duration ttl) {
            this.key = key;
            this.ttl = ttl;
        }
    }

    /**
     * <h3>Redis에 인기글 목록 캐싱</h3>
     *
     * @param popularPosts 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    public void cachePopularPosts(PopularPostType type, List<SimplePostDTO> popularPosts) {
        try {
            redisTemplate.opsForValue().set(type.getKey(), popularPosts, type.getTtl());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR);
        }
    }

    /**
     * <h3>Redis에서 인기글 목록 조회</h3>
     *
     * @return 캐시된 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public List<SimplePostDTO> getCachedPopularPosts(PopularPostType type) {

        if (!hasPopularPostsCache(type)) {
            postService.updateRealtimePopularPosts();
        }

        try {
            Object cached = redisTemplate.opsForValue().get(type.getKey());
            if (cached instanceof List) {
                return (List<SimplePostDTO>) cached;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR);
        }
        return Collections.emptyList();
    }

    /**
     * <h3>Redis에서 인기글 캐시 삭제</h3>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    public void deletePopularPostsCache(PopularPostType type) {
        try {
            redisTemplate.delete(type.getKey());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR);
        }
    }

    /**
     * <h3>Redis에 인기글 캐시가 존재하는지 확인</h3>
     *
     * @return 캐시 존재 여부
     * @author Jaeik
     * @since 1.0.0
     */
    public boolean hasPopularPostsCache(PopularPostType type) {
        try {
            return redisTemplate.hasKey(type.getKey());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR);
        }
    }

    /**
     * <h3>모든 인기글 캐시 삭제</h3>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    public void deleteAllPopularPostsCache() {
        for (PopularPostType type : PopularPostType.values()) {
            deletePopularPostsCache(type);
        }
    }
}