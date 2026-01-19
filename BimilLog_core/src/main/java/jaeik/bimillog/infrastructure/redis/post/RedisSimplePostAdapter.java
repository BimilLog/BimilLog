package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

/**
 * <h2>레디스 게시글 캐시 티어1 저장소 어댑터</h2>
 * <p>게시글 목록 캐시를 개별 String 구조로 관리합니다.</p>
 * <p>각 게시글은 개별 TTL을 가지며, MGET으로 한 번에 조회합니다.</p>
 * <p>Redis TTL 기반 PER(Probabilistic Early Refresh)을 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisSimplePostAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * PER의 expiry gap (초 단위)
     * <p>TTL 마지막 60초 동안 확률적으로 캐시를 갱신합니다.</p>
     */
    private static final long PER_EXPIRY_GAP_SECONDS = 60;

    /**
     * <h3>MGET으로 캐시된 게시글 조회</h3>
     * <p>여러 postId에 대한 캐시를 한 번의 요청으로 조회합니다.</p>
     *
     * @param type    캐시 유형
     * @param postIds 조회할 게시글 ID 목록
     * @return postId를 키로, PostSimpleDetail을 값으로 하는 Map (캐시 미스는 포함되지 않음)
     */
    public Map<Long, PostSimpleDetail> getCachedPosts(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = getSimplePostKeys(type, postIds);
        String logPrefix = "post:" + type.name().toLowerCase() + ":simple";

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            CacheMetricsLogger.miss(log, logPrefix, "mget", "null_response");
            return Collections.emptyMap();
        }

        Map<Long, PostSimpleDetail> result = new HashMap<>();
        int hitCount = 0;

        for (int i = 0; i < postIds.size(); i++) {
            Object value = values.get(i);
            if (value instanceof PostSimpleDetail post) {
                result.put(postIds.get(i), post);
                hitCount++;
            }
        }

        if (hitCount > 0) {
            CacheMetricsLogger.hit(log, logPrefix, "mget", hitCount);
        }
        if (hitCount < postIds.size()) {
            CacheMetricsLogger.miss(log, logPrefix, "mget", "partial_miss:" + (postIds.size() - hitCount));
        }

        return result;
    }

    /**
     * <h3>PER 대상 게시글 ID 필터링 (Redis TTL 기반)</h3>
     * <p>캐시된 게시글 중 TTL이 60초 미만인 게시글 ID를 확률적으로 반환합니다.</p>
     *
     * @param type      캐시 유형
     * @param postIds   캐시된 게시글 ID 목록
     * @return 갱신이 필요한 게시글 ID 목록
     */
    public List<Long> filterRefreshNeeded(PostCacheFlag type, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> refreshNeeded = new ArrayList<>();

        for (Long postId : postIds) {
            String key = getSimplePostKey(type, postId);
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            if (ttl > 0 && shouldRefresh(ttl)) {
                refreshNeeded.add(postId);
            }
        }

        return refreshNeeded;
    }

    /**
     * <h3>PER 기반 갱신 필요 여부 판단</h3>
     * <p>TTL 마지막 60초 동안 확률적으로 true를 반환합니다.</p>
     *
     * @param ttlSeconds 남은 TTL (초)
     * @return 갱신이 필요하면 true
     */
    private boolean shouldRefresh(long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return true;
        }
        if (ttlSeconds < PER_EXPIRY_GAP_SECONDS) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            return ttlSeconds - (randomFactor * PER_EXPIRY_GAP_SECONDS) <= 0;
        }
        return false;
    }

    /**
     * <h3>캐시 미스 게시글 ID 필터링</h3>
     * <p>요청한 postId 중 캐시에 없는 ID를 반환합니다.</p>
     *
     * @param requestedIds 요청한 게시글 ID 목록
     * @param cachedPosts  캐시된 게시글 Map
     * @return 캐시 미스된 게시글 ID 목록
     */
    public List<Long> filterMissedIds(List<Long> requestedIds, Map<Long, PostSimpleDetail> cachedPosts) {
        return requestedIds.stream()
                .filter(id -> !cachedPosts.containsKey(id))
                .toList();
    }

    /**
     * <h3>개별 게시글 캐시 저장</h3>
     *
     * @param type 캐시 유형
     * @param post 저장할 게시글
     */
    public void cachePost(PostCacheFlag type, PostSimpleDetail post) {
        if (post == null || post.getId() == null) {
            return;
        }

        String key = getSimplePostKey(type, post.getId());
        redisTemplate.opsForValue().set(key, post, POST_CACHE_TTL.toSeconds(), TimeUnit.SECONDS);
        log.debug("[CACHE_WRITE] key={}, postId={}", key, post.getId());
    }

    /**
     * <h3>여러 게시글 캐시 저장</h3>
     *
     * @param type  캐시 유형
     * @param posts 저장할 게시글 목록
     */
    public void cachePosts(PostCacheFlag type, List<PostSimpleDetail> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        log.info("[CACHE_WRITE] START - type={}, count={}", type, posts.size());

        for (PostSimpleDetail post : posts) {
            if (post != null && post.getId() != null) {
                String key = getSimplePostKey(type, post.getId());
                redisTemplate.opsForValue().set(key, post, POST_CACHE_TTL.toSeconds(), TimeUnit.SECONDS);
            }
        }

        log.info("[CACHE_WRITE] SUCCESS - type={}, count={}, ttl={}min", type, posts.size(), POST_CACHE_TTL.toMinutes());
    }

    /**
     * <h3>단일 캐시 삭제</h3>
     * <p>모든 캐시 유형에서 특정 postId의 캐시를 삭제합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePostFromCache(Long postId) {
        if (postId == null) {
            return;
        }

        List<String> keysToDelete = Arrays.stream(PostCacheFlag.values())
                .map(type -> getSimplePostKey(type, postId))
                .toList();

        redisTemplate.delete(keysToDelete);
        log.debug("[CACHE_DELETE] postId={}, keys={}", postId, keysToDelete.size());
    }

    /**
     * <h3>특정 타입의 단일 캐시 삭제</h3>
     *
     * @param type   캐시 유형
     * @param postId 제거할 게시글 ID
     */
    public void removePostFromCache(PostCacheFlag type, Long postId) {
        if (postId == null) {
            return;
        }

        String key = getSimplePostKey(type, postId);
        redisTemplate.delete(key);
        log.debug("[CACHE_DELETE] type={}, postId={}", type, postId);
    }

    /**
     * <h3>PostSimpleDetail Map을 리스트로 변환</h3>
     * <p>요청된 순서를 유지하며 변환합니다.</p>
     *
     * @param orderedIds  정렬 순서를 가진 ID 목록
     * @param cachedPosts 캐시된 게시글 Map
     * @return 정렬된 PostSimpleDetail 목록
     */
    public List<PostSimpleDetail> toOrderedList(List<Long> orderedIds, Map<Long, PostSimpleDetail> cachedPosts) {
        return orderedIds.stream()
                .map(cachedPosts::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
