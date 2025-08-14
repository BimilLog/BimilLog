package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence.post.cache;

import jaeik.growfarm.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Redis 캐시 조회 어댑터</h2>
 * <p>Redis를 사용하여 게시글 캐시 데이터를 관리하는 영속성 어댑터입니다.</p>
 * <p>PostCacheQueryPort 인터페이스를 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class PostCacheQueryAdapter implements PostCacheQueryPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";

    /**
     * <h3>RedisCacheAdapter 생성자</h3>
     * <p>RedisTemplate을 주입받아 캐시 메타데이터를 초기화합니다.</p>
     *
     * @param redisTemplate Redis 작업을 위한 템플릿
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCacheQueryAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cacheMetadataMap = new EnumMap<>(PostCacheFlag.class);
        cacheMetadataMap.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(30)));
        cacheMetadataMap.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofDays(7)));
    }

    private record CacheMetadata(String key, Duration ttl) {}

    /**
     * <h3>캐시 메타데이터 조회</h3>
     * <p>주어진 캐시 유형에 해당하는 메타데이터를 조회합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시 메타데이터
     * @throws CustomException 알 수 없는 PostCacheFlag 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private CacheMetadata getCacheMetadata(PostCacheFlag type) {
        CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR.getStatus(), "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    @Override
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            return redisTemplate.hasKey(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SimplePostResDTO> getCachedPostList(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
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

    @Override
    public FullPostResDTO getCachedPost(Long postId) {
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
}
