package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.POSTIDS_TTL_WEEKLY_LEGEND;
import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.getPostIdsStorageKey;

/**
 * <h2>레디스 게시글 캐시 티어2 저장소 어댑터</h2>
 * <p>실시간 인기글 저장소를 제외한 주간, 전설, 공지사항의 ID목록 저장소를 관리한다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisTier2PostStoreAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>글 ID 목록 조회</h3>
     * <p>캐시 미스 발생 시 복구를 위해 영구 저장된 postId 목록을 조회합니다.</p>
     *
     * @param type 조회할 인기글 캐시 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 저장된 게시글 ID 목록 (없으면 빈 리스트)
     */
    public List<Long> getStoredPostIds(PostCacheFlag type) {
        String postIdsKey = getPostIdsStorageKey(type);
        Set<Object> postIds;

        if (type == PostCacheFlag.NOTICE) {
            postIds = redisTemplate.opsForSet().members(postIdsKey); // 공지사항: Set에서 조회
        } else {
            // 주간/레전드: Sorted Set에서 조회 (점수 오름차순 = DB 추출 순서)
            postIds = redisTemplate.opsForZSet().range(postIdsKey, 0, -1);
        }

        if (postIds == null || postIds.isEmpty()) {
            CacheMetricsLogger.miss(log, "post:ids:" + type.name().toLowerCase(),
                    postIdsKey, "post_ids_empty");
            return Collections.emptyList();
        }

        List<Long> ids = postIds.stream()
                .map(Object::toString)
                .map(Long::valueOf)
                .toList();
        CacheMetricsLogger.hit(log, "post:ids:" + type.name().toLowerCase(),
                postIdsKey, ids.size());
        return ids;
    }

    /**
     * <h3>캐시 저장</h3>
     * <p>인기글 postId 목록 저장합니다.</p>
     *
     * @param type  캐시할 게시글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록 (이미 인기도 순으로 정렬됨)
     */
    public void cachePostIdsOnly(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        String postIdsKey = getPostIdsStorageKey(type);

        // 기존 postIds 캐시 삭제
        redisTemplate.delete(postIdsKey);

        if (type == PostCacheFlag.NOTICE) {
            // 공지사항: Set으로 저장 (순서 불필요)
            for (Long postId : postIds) {
                redisTemplate.opsForSet().add(postIdsKey, postId.toString());
            }
        } else {
            // 주간/레전드: Sorted Set으로 저장 (점수 = DB 추출 순서)
            double score = 1.0;
            for (Long postId : postIds) {
                redisTemplate.opsForZSet().add(postIdsKey, postId.toString(), score++);
            }
        }

        // TTL 설정: 주간/레전드는 1일, 공지는 영구
        if (type != PostCacheFlag.NOTICE) {
            redisTemplate.expire(postIdsKey, POSTIDS_TTL_WEEKLY_LEGEND);
        }
    }

    /**
     * <h3>공지사항 ID 저장</h3>
     * <p>postIds 영구 저장소에 게시글 ID를 추가합니다 (Set 사용).</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 추가할 게시글 ID
     */
    public void addPostIdToStorage(PostCacheFlag type, Long postId) {
        String postIdsKey = getPostIdsStorageKey(type);
        redisTemplate.opsForSet().add(postIdsKey, postId.toString());
    }

    /**
     * <h3>캐시 삭제</h3>
     * <p>모든 postIds 영구 저장소에서 게시글 ID를 제거합니다 (Sorted Set 또는 Set).</p>
     * <p>REALTIME을 제외한 모든 PostCacheFlag를 순회하며 저장소에서 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
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
}
