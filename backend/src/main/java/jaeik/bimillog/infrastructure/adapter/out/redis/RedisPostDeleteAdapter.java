package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.post.application.port.out.RedisPostDeletePort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static jaeik.bimillog.infrastructure.adapter.out.redis.RedisPostKeys.*;


@Component
@RequiredArgsConstructor
public class RedisPostDeleteAdapter implements RedisPostDeletePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, CacheMetadata> cacheMetadataMap = RedisPostKeys.CACHE_METADATA_MAP;


    private RedisPostKeys.CacheMetadata getCacheMetadata(PostCacheFlag type) {
        RedisPostKeys.CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }


    /**
     * <h3>postIds 저장소에서 단일 게시글 제거</h3>
     * <p>postIds 영구 저장소에서 게시글 ID를 제거합니다 (LREM).</p>
     * <p>공지사항 해제 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void removePostIdFromStorage(PostCacheFlag type, Long postId) {
        String postIdsKey = getPostIdsStorageKey(type);
        try {
            // LREM: 0 = 모든 매칭 요소 제거
            redisTemplate.opsForList().remove(postIdsKey, 0, postId.toString());
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>단일 게시글 캐시 무효화</h3>
     * <p>특정 게시글의 캐시 데이터를 Redis에서 삭제합니다 (라이트 어라운드 패턴).</p>
     *
     * @param postId 캐시를 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteSinglePostCache(Long postId) {
        String detailKey = getPostDetailKey(postId);
        try {
            redisTemplate.delete(detailKey);
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_DELETE_ERROR, e);
        }
    }
}
