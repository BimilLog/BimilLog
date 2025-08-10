package jaeik.growfarm.service.redis;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.cache.PostCacheRepository;
import jaeik.growfarm.service.post.PostScheduledService;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
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
 * <p>
 * 실시간, 주간, 레전드 인기 게시글 목록을 하나의 서비스에서 관리한다.
 * </p>
 * <p>
 * Redis에 캐싱하고, 조회, 삭제 등의 작업을 수행한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
public class RedisPostService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostScheduledService postScheduledService;
    private final PostCacheRepository postCacheRepository;

    public RedisPostService(RedisTemplate<String, Object> redisTemplate, 
                            @Lazy PostScheduledService postScheduledService,
                            PostCacheRepository postCacheRepository) {
        this.redisTemplate = redisTemplate;
        this.postScheduledService = postScheduledService;
        this.postCacheRepository = postCacheRepository;
    }

    /**
     * <h3>캐시 유형</h3>
     * <p>
     * Redis에 저장되는 캐시글 목록의 유형을 정의한다.
     * </p>
     * <p>
     * 각 유형은 Redis 키와 TTL(유효 기간)을 포함한다.
     * </p>
     * 
     * @author Jaeik
     * @version 2.0.0
     */
    @Getter
    public enum CachePostType {
        REALTIME("cache:posts:realtime", Duration.ofMinutes(30)),
        WEEKLY("cache:posts:weekly", Duration.ofDays(1)),
        LEGEND("cache:posts:legend", Duration.ofDays(1)),
        NOTICE("cache:posts:notice", Duration.ofDays(7));

        private final String key;
        private final Duration ttl;

        CachePostType(String key, Duration ttl) {
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
    public void cachePopularPosts(CachePostType type, List<SimplePostResDTO> popularPosts) {
        try {
            redisTemplate.opsForValue().set(type.getKey(), popularPosts, type.getTtl());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
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
    public List<SimplePostResDTO> getCachedPopularPosts(CachePostType type) {

        if (!hasPopularPostsCache(type)) {
            switch (type) {
                case REALTIME -> postScheduledService.updateRealtimePopularPosts();
                case WEEKLY -> postScheduledService.updateWeeklyPopularPosts();
                case LEGEND -> postScheduledService.updateLegendPopularPosts();
                case NOTICE -> {
                    // 공지사항은 DB에서 직접 조회하여 캐시에 저장
                    List<SimplePostResDTO> noticePosts = postCacheRepository.findNoticePost();
                    cachePopularPosts(CachePostType.NOTICE, noticePosts);
                }
            }
        }

        try {
            Object cached = redisTemplate.opsForValue().get(type.getKey());
            if (cached instanceof List) {
                return (List<SimplePostResDTO>) cached;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return Collections.emptyList();
    }

    /**
     * <h3>Redis에서 공지사항 목록 조회 (통합 메서드 사용)</h3>
     * <p>
     * getCachedPopularPosts(CachePostType.NOTICE)를 사용하도록 변경
     * </p>
     *
     * @return 캐시된 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<SimplePostResDTO> getCachedNoticePosts() {
        return getCachedPopularPosts(CachePostType.NOTICE);
    }

    /**
     * <h3>Redis에서 인기글 캐시 삭제</h3>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    public void deletePopularPostsCache(CachePostType type) {
        try {
            redisTemplate.delete(type.getKey());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }

    /**
     * <h3>Redis에 인기글 캐시가 존재하는지 확인</h3>
     *
     * @return 캐시 존재 여부.
     * @author Jaeik
     * @since 1.0.0
     */
    public boolean hasPopularPostsCache(CachePostType type) {
        try {
            return redisTemplate.hasKey(type.getKey());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>공지사항 캐시 삭제 (통합 메서드 사용)</h3>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteNoticePostsCache() {
        deletePopularPostsCache(CachePostType.NOTICE);
    }

    /**
     * <h3>모든 인기글 캐시 삭제</h3>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    public void deleteAllPopularPostsCache() {
        for (CachePostType type : CachePostType.values()) {
            deletePopularPostsCache(type);
        }
    }
}