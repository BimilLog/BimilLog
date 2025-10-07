package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.post.application.port.out.RedisPostDeletePort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * <h3>캐시 삭제</h3>
     * <p>캐시를 삭제합니다. type이 null이면 특정 게시글의 모든 캐시를 삭제하고,</p>
     * <p>type이 지정되면 해당 타입의 목록 캐시와 관련 상세 캐시를 삭제합니다.</p>
     *
     * @param type   캐시할 게시글 유형 (null이면 특정 게시글 삭제 모드)
     * @param postId 게시글 ID (type이 null일 때만 사용)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteCache(PostCacheFlag type, Long postId, PostCacheFlag... targetTypes) {
        try {
            if (type == null && targetTypes.length == 0) {
                // 특정 게시글의 모든 캐시 삭제
                deleteSpecificPostCache(postId);
            } else if (type == null) {
                // 특정 게시글의 지정된 캐시만 삭제 (성능 최적화)
                deleteSpecificPostCache(postId, targetTypes);
            } else {
                // 특정 타입의 캐시와 관련 상세 캐시 삭제
                deleteTypeCacheWithDetails(type);
            }
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_DELETE_ERROR, e);
        }
    }

    private void deleteSpecificPostCache(Long postId) {
        // 1. 상세 캐시 삭제
        String detailKey = getPostDetailKey(postId);
        redisTemplate.delete(detailKey);

        // 2. 모든 목록 캐시에서 해당 게시글 제거
        String postIdStr = postId.toString();
        for (PostCacheFlag type : PostCacheFlag.getPopularPostTypes()) {
            RedisPostKeys.CacheMetadata metadata = getCacheMetadata(type);
            // LREM: 0 = 모든 매칭 요소 제거
            redisTemplate.opsForList().remove(metadata.key(), 0, postIdStr);
        }
    }

    private void deleteTypeCacheWithDetails(PostCacheFlag type) {
        RedisPostKeys.CacheMetadata metadata = getCacheMetadata(type);

        // 1. 목록 캐시에서 게시글 ID들을 먼저 조회
        List<Object> postIds = redisTemplate.opsForList().range(metadata.key(), 0, -1);

        // 2. 목록 캐시 삭제
        redisTemplate.delete(metadata.key());

        // 3. 해당 타입에 속했던 게시글들의 상세 캐시도 삭제
        if (postIds != null && !postIds.isEmpty()) {
            List<String> detailKeys = postIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .map(RedisPostKeys::getPostDetailKey)
                    .collect(Collectors.toList());
            redisTemplate.delete(detailKeys);
        }
    }

    /**
     * <h3>특정 게시글의 지정된 캐시만 삭제</h3>
     * <p>성능 최적화를 위해 지정된 캐시 타입에서만 게시글을 삭제합니다.</p>
     * <p>공지 해제 시 NOTICE 캐시만 스캔하도록 최적화하는데 사용됩니다.</p>
     *
     * @param postId      삭제할 게시글 ID
     * @param targetTypes 삭제할 대상 캐시 타입들
     * @author Jaeik
     * @since 2.0.0
     */
    private void deleteSpecificPostCache(Long postId, PostCacheFlag[] targetTypes) {
        // 1. 상세 캐시 삭제
        String detailKey = getPostDetailKey(postId);
        redisTemplate.delete(detailKey);

        // 2. 지정된 목록 캐시에서만 해당 게시글 제거
        String postIdStr = postId.toString();
        for (PostCacheFlag type : targetTypes) {
            RedisPostKeys.CacheMetadata metadata = getCacheMetadata(type);
            // LREM: 0 = 모든 매칭 요소 제거
            redisTemplate.opsForList().remove(metadata.key(), 0, postIdStr);
        }
    }
}
