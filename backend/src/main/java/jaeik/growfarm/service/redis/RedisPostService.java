package jaeik.growfarm.service.redis;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.cache.PostCacheRepository;
import jaeik.growfarm.service.post.PostScheduledService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import jaeik.growfarm.entity.post.PostCacheFlag; // 기존 엔티티의 PostCacheFlag 임포트

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.EnumMap; // EnumMap을 사용하여 Enum과 값 매핑

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

    // Redis 키와 TTL을 저장할 내부 맵
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";
    private static final Duration FULL_POST_CACHE_TTL = Duration.ofDays(1);


    public RedisPostService(RedisTemplate<String, Object> redisTemplate,
                            @Lazy PostScheduledService postScheduledService,
                            PostCacheRepository postCacheRepository) {
        this.redisTemplate = redisTemplate;
        this.postScheduledService = postScheduledService;
        this.postCacheRepository = postCacheRepository;

        // PostCacheFlag enum에 따른 Redis 키와 TTL 초기화
        this.cacheMetadataMap = new EnumMap<>(PostCacheFlag.class);
        cacheMetadataMap.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(30)));
        cacheMetadataMap.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofDays(7)));
    }

    /**
     * <h3>캐시 메타데이터</h3>
     * <p>
     * 각 PostCacheFlag에 대한 Redis 키와 TTL 정보를 담는 내부 클래스
     * </p>
     *
     * @author Jaeik
     * @version 2.0.0
     */
    private record CacheMetadata(String key, Duration ttl) {
    }

    /**
     * <h3>PostCacheFlag에 해당하는 캐시 메타데이터 조회</h3>
     *
     * @param type 게시글 캐시 플래그
     * @return 캐시 메타데이터
     */
    private CacheMetadata getCacheMetadata(PostCacheFlag type) {
        CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR.getStatus(), "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    /**
     * <h3>Redis에 글 목록 캐싱</h3>
     *
     * @param type       캐시할 게시글의 유형
     * @param cachePosts 캐시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public void cachePosts(PostCacheFlag type, List<SimplePostResDTO> cachePosts) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            redisTemplate.opsForValue().set(metadata.key(), cachePosts, metadata.ttl());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>Redis에서 캐시글 목록 조회</h3>
     *
     * @param type 조회할 게시글 캐시 플래그
     * @return 캐시된 인기글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public List<SimplePostResDTO> getCachedPopularPosts(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);

        if (!hasPopularPostsCache(type)) {
            // 캐시가 없으면 해당 유형에 따라 업데이트 로직 수행
            switch (type) {
                case REALTIME -> postScheduledService.updateRealtimePopularPosts();
                case WEEKLY -> postScheduledService.updateWeeklyPopularPosts();
                case LEGEND -> postScheduledService.updateLegendPopularPosts();
                case NOTICE -> {
                    List<SimplePostResDTO> noticePosts = postCacheRepository.findNoticePost();
                    cachePosts(PostCacheFlag.NOTICE, noticePosts);
                }
            }
        }

        try {
            Object cached = redisTemplate.opsForValue().get(metadata.key());
            if (cached instanceof List) {
                return (List<SimplePostResDTO>) cached;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return Collections.emptyList();
    }

    /**
     * <h3>Redis에서 공지사항 목록 조회</h3>
     * <p>
     * getCachedPopularPosts(PostCacheFlag.NOTICE)를 사용
     * </p>
     *
     * @return 캐시된 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<SimplePostResDTO> getCachedNoticePosts() {
        return getCachedPopularPosts(PostCacheFlag.NOTICE);
    }

    /**
     * <h3>Redis에서 인기글 캐시 삭제</h3>
     *
     * @param type 삭제할 게시글 캐시 플래그
     * @author Jaeik
     * @since 1.0.0
     */
    public void deletePopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            redisTemplate.delete(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }

    /**
     * <h3>Redis에 인기글 캐시가 존재하는지 확인</h3>
     *
     * @param type 확인할 게시글 캐시 플래그
     * @return 캐시 존재 여부.
     * @author Jaeik
     * @since 1.0.0
     */
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.hasKey(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    /**
     * <h3>공지사항 캐시 삭제</h3>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteNoticePostsCache() {
        deletePopularPostsCache(PostCacheFlag.NOTICE);
    }

    /**
     * <h3>모든 인기글 캐시 삭제</h3>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    public void deleteAllPopularPostsCache() {
        for (PostCacheFlag type : PostCacheFlag.values()) {
            deletePopularPostsCache(type);
        }
    }

    /**
     * <h3>게시글 상세 정보 캐싱</h3>
     *
     * @param post 캐시할 게시글 상세 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public void cacheFullPost(FullPostResDTO post) {
        String key = FULL_POST_CACHE_PREFIX + post.getPostId();
        try {
            redisTemplate.opsForValue().set(key, post, FULL_POST_CACHE_TTL);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>캐시된 게시글 상세 정보 조회</h3>
     *
     * @param postId 조회할 게시글 ID
     * @return 캐시된 게시글 상세 정보 DTO (없으면 null)
     * @author Jaeik
     * @since 2.0.0
     */
    public FullPostResDTO getCachedFullPost(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof FullPostResDTO) {
                return (FullPostResDTO) cached;
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
        return null;
    }

    /**
     * <h3>게시글 상세 정보 캐시 삭제</h3>
     *
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteFullPostCache(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
}
