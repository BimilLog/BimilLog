package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.domain.post.application.port.out.RedisPostSavePort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static jaeik.bimillog.infrastructure.adapter.out.redis.RedisPostKeys.*;

/**
 * <h2>게시글 캐시 명령 어댑터</h2>
 * <p>게시글 캐시 명령 포트의 Redis 구현체입니다.</p>
 * <p>인기글 캐시 데이터 생성, 수정, 삭제</p>
 * <p>인기글 플래그 설정 및 초기화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class RedisPostSaveAdapter implements RedisPostSavePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<PostCacheFlag, RedisPostKeys.CacheMetadata> cacheMetadataMap = RedisPostKeys.CACHE_METADATA_MAP;

    /**
     * <h3>캐시 메타데이터 조회</h3>
     * <p>주어진 캐시 유형에 해당하는 메타데이터를 조회합니다.</p>
     *
     * @param type 게시글 캐시 유형
     * @return 캐시 메타데이터
     * @throws PostCustomException 알 수 없는 PostCacheFlag 유형인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private RedisPostKeys.CacheMetadata getCacheMetadata(PostCacheFlag type) {
        RedisPostKeys.CacheMetadata metadata = cacheMetadataMap.get(type);
        if (metadata == null) {
            throw new PostCustomException(PostErrorCode.REDIS_READ_ERROR, "Unknown PostCacheFlag type: " + type);
        }
        return metadata;
    }

    /**
     * <h3>인기글 postId 영구 저장</h3>
     * <p>인기글 postId 목록만 Redis List에 영구 또는 긴 TTL로 저장합니다.</p>
     * <p>목록 캐시 TTL 만료 시 복구용으로 사용됩니다.</p>
     *
     * @param type  캐시할 게시글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록 (이미 인기도 순으로 정렬됨)
     * @throws PostCustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostIdsOnly(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        String postIdsKey = getPostIdsStorageKey(type);

        try {
            // 기존 postIds 캐시 삭제
            redisTemplate.delete(postIdsKey);

            // List에 postId만 저장 (RPUSH로 순서대로 삽입)
            for (Long postId : postIds) {
                redisTemplate.opsForList().rightPush(postIdsKey, postId.toString());
            }

            // TTL 설정: 주간/레전드는 1일, 공지는 영구
            if (type != PostCacheFlag.NOTICE) {
                // 주간/레전드는 1일
                redisTemplate.expire(postIdsKey, POSTIDS_TTL_WEEKLY_LEGEND);
            }

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>인기글 postId 목록 캐싱</h3>
     * <p>인기글 postId 목록만 Redis List에 저장합니다 (상세 정보는 저장하지 않음).</p>
     * <p>메모리 효율을 위해 postId만 저장하고, 조회 시 캐시 어사이드 패턴으로 PostDetail을 조회합니다.</p>
     *
     * @param type  캐시할 게시글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록 (이미 인기도 순으로 정렬됨)
     * @throws PostCustomException Redis 쓰기 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostIds(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        RedisPostKeys.CacheMetadata metadata = getCacheMetadata(type);

        try {
            // List에 postId만 저장 (RPUSH로 순서대로 삽입)
            String listKey = metadata.key();
            for (Long postId : postIds) {
                redisTemplate.opsForList().rightPush(listKey, postId.toString());
            }
            // TTL 설정
            redisTemplate.expire(listKey, metadata.ttl());

        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>단일 게시글 상세 정보 캐싱</h3>
     * <p>게시글 상세 정보를 Redis 캐시에 저장합니다 (캐시 어사이드 패턴).</p>
     *
     * @param postDetail 캐시할 게시글 상세 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void cachePostDetail(PostDetail postDetail) {
        String key = getPostDetailKey(postDetail.getId());
        try {
            redisTemplate.opsForValue().set(key, postDetail, FULL_POST_CACHE_TTL);
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }

    /**
     * <h3>postIds 저장소에 단일 게시글 추가</h3>
     * <p>postIds 영구 저장소의 앞에 게시글 ID를 추가합니다 (LPUSH).</p>
     * <p>공지사항 설정 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 추가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void addPostIdToStorage(PostCacheFlag type, Long postId) {
        String postIdsKey = getPostIdsStorageKey(type);
        try {
            redisTemplate.opsForList().leftPush(postIdsKey, postId.toString());
        } catch (Exception e) {
            throw new PostCustomException(PostErrorCode.REDIS_WRITE_ERROR, e);
        }
    }
}
