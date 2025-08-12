package jaeik.growfarm.domain.post.infrastructure.adapter.out.redis;

import jaeik.growfarm.domain.post.application.port.out.LoadPostCachePort;
import jaeik.growfarm.domain.post.application.port.out.ManagePostCachePort;
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

@Component
public class RedisCacheAdapter implements ManagePostCachePort, LoadPostCachePort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap;
    private static final String FULL_POST_CACHE_PREFIX = "cache:post:";
    private static final Duration FULL_POST_CACHE_TTL = Duration.ofDays(1);

    public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cacheMetadataMap = new EnumMap<>(PostCacheFlag.class);
        cacheMetadataMap.put(PostCacheFlag.REALTIME, new CacheMetadata("cache:posts:realtime", Duration.ofMinutes(30)));
        cacheMetadataMap.put(PostCacheFlag.WEEKLY, new CacheMetadata("cache:posts:weekly", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.LEGEND, new CacheMetadata("cache:posts:legend", Duration.ofDays(1)));
        cacheMetadataMap.put(PostCacheFlag.NOTICE, new CacheMetadata("cache:posts:notice", Duration.ofDays(7)));
    }

    private record CacheMetadata(String key, Duration ttl) {}

    private CacheMetadata getCacheMetadata(PostCacheFlag type) {
        CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR.getStatus(), "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    @Override
    public void cachePosts(PostCacheFlag type, List<SimplePostResDTO> cachePosts) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            redisTemplate.opsForValue().set(metadata.key(), cachePosts, metadata.ttl());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<SimplePostResDTO> getCachedPopularPosts(PostCacheFlag type) {
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
    public void deletePopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            redisTemplate.delete(metadata.key());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }

    @Override
    public boolean hasPopularPostsCache(PostCacheFlag type) {
        CacheMetadata metadata = getCacheMetadata(type);
        try {
            Boolean hasKey = redisTemplate.hasKey(metadata.key());
            return hasKey != null && hasKey;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_READ_ERROR, e);
        }
    }

    @Override
    public void cacheFullPost(FullPostResDTO post) {
        String key = FULL_POST_CACHE_PREFIX + post.getId();
        try {
            redisTemplate.opsForValue().set(key, post, FULL_POST_CACHE_TTL);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    @Override
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

    @Override
    public void deleteFullPostCache(Long postId) {
        String key = FULL_POST_CACHE_PREFIX + postId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
}
