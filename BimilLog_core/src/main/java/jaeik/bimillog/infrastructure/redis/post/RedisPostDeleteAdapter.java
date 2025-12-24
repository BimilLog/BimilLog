package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;


@Component
@RequiredArgsConstructor
public class RedisPostDeleteAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>postIds 저장소에서 단일 게시글 제거</h3>
     * <p>모든 postIds 영구 저장소에서 게시글 ID를 제거합니다 (Sorted Set 또는 Set).</p>
     * <p>REALTIME을 제외한 모든 PostCacheFlag를 순회하며 저장소에서 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void removePostIdFromStorage(Long postId) {
        for (PostCacheFlag type : PostCacheFlag.values()) {
            if (type == PostCacheFlag.REALTIME) {
                continue;
            }
            String postIdsKey = getPostIdsStorageKey(type);
            if (type == PostCacheFlag.NOTICE) {
                // 공지사항: Set에서 제거
                redisTemplate.opsForSet().remove(postIdsKey, postId.toString());
            } else {
                // 주간/레전드: Sorted Set에서 제거
                redisTemplate.opsForZSet().remove(postIdsKey, postId.toString());
            }
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
    public void deleteSinglePostCache(Long postId) {
        String detailKey = getPostDetailKey(postId);
        redisTemplate.delete(detailKey);
    }

    /**
     * <h3>게시글 목록 캐시에서 단일 게시글 제거 (Hash 필드 삭제)</h3>
     * <p>모든 Redis Hash에서 특정 postId의 PostSimpleDetail을 삭제합니다.</p>
     * <p>모든 PostCacheFlag를 순회하며 각 타입의 목록 캐시에서 제거합니다.</p>
     * <p>게시글 수정/삭제 시 목록 캐시 무효화를 위해 호출됩니다.</p>
     *
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void removePostFromListCache(Long postId) {
        for (PostCacheFlag type : PostCacheFlag.values()) {
            String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();
            redisTemplate.opsForHash().delete(hashKey, postId.toString());
        }
    }

    /**
     * <h3>실시간 인기글 점수 저장소에서 게시글 제거</h3>
     * <p>score:realtime Sorted Set에서 특정 postId를 삭제합니다.</p>
     * <p>게시글 삭제 시 실시간 인기글 점수 정리를 위해 호출됩니다.</p>
     *
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void removePostIdFromRealtimeScore(Long postId) {
        redisTemplate.opsForZSet().remove(REALTIME_POST_SCORE_KEY, postId.toString());
    }

    /**
     * <h3>게시글 목록 캐시 전체 삭제</h3>
     * <p>특정 캐시 유형의 posts:{type} Hash 전체를 삭제합니다.</p>
     * <p>스케줄러 재실행 시 기존 목록 캐시를 초기화하기 위해 호출됩니다.</p>
     *
     * @param type 삭제할 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    public void clearPostListCache(PostCacheFlag type) {
        String hashKey = RedisPostKeys.CACHE_METADATA_MAP.get(type).key();
        redisTemplate.delete(hashKey);
    }
}
