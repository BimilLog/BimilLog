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
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

/**
 * <h2>레디스 게시글 캐시 티어1 저장소 어댑터</h2>
 * <p>게시글 목록 캐시를 타입별 Hash 구조로 관리합니다.</p>
 * <p>각 타입별로 하나의 Hash 키를 사용하여 N+1 문제를 해결하고 PER을 단순화합니다.</p>
 * <p>키 형식: post:{type}:simple (Hash, field=postId, value=PostSimpleDetail)</p>
 *
 * @author Jaeik
 * @version 2.7.0
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
     * <h3>HGETALL로 Hash 전체 캐시 조회</h3>
     * <p>타입별 Hash에서 모든 게시글을 한 번에 조회합니다.</p>
     *
     * @param type 캐시 유형
     * @return postId를 키로, PostSimpleDetail을 값으로 하는 Map (캐시가 없으면 빈 Map)
     */
    public Map<Long, PostSimpleDetail> getAllCachedPosts(PostCacheFlag type) {
        String hashKey = getSimplePostHashKey(type);
        String logPrefix = "post:" + type.name().toLowerCase() + ":simple";

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
        if (entries.isEmpty()) {
            CacheMetricsLogger.miss(log, logPrefix, "hgetall", "empty");
            return Collections.emptyMap();
        }

        Map<Long, PostSimpleDetail> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getValue() instanceof PostSimpleDetail post) {
                Long postId = Long.parseLong(entry.getKey().toString());
                result.put(postId, post);
            }
        }

        CacheMetricsLogger.hit(log, logPrefix, "hgetall", result.size());
        return result;
    }

    /**
     * <h3>Hash 전체 TTL 기반 PER 갱신 필요 여부 판단</h3>
     * <p>Hash 키의 TTL이 60초 미만이면 확률적으로 true를 반환합니다.</p>
     *
     * @param type 캐시 유형
     * @return 갱신이 필요하면 true
     */
    public boolean shouldRefreshHash(PostCacheFlag type) {
        String hashKey = getSimplePostHashKey(type);
        long ttl = redisTemplate.getExpire(hashKey, TimeUnit.SECONDS);

        if (ttl < 0) {
            return false; // 키가 없거나 TTL이 없음
        }

        return shouldRefresh(ttl);
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
     * <h3>여러 게시글 Hash 캐시 저장 (HMSET 1회)</h3>
     * <p>HMSET으로 한 번에 저장하여 N+1 문제를 해결합니다.</p>
     *
     * @param type  캐시 유형
     * @param posts 저장할 게시글 목록
     */
    public void cachePosts(PostCacheFlag type, List<PostSimpleDetail> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        String hashKey = getSimplePostHashKey(type);

        log.info("[CACHE_WRITE] START - hashKey={}, count={}", hashKey, posts.size());

        // Map<String, PostSimpleDetail>로 변환
        Map<String, PostSimpleDetail> hashData = posts.stream()
                .filter(post -> post != null && post.getId() != null)
                .collect(Collectors.toMap(
                        p -> p.getId().toString(),
                        p -> p
                ));

        // HMSET 1회로 저장
        redisTemplate.opsForHash().putAll(hashKey, hashData);
        redisTemplate.expire(hashKey, POST_CACHE_TTL);

        log.info("[CACHE_WRITE] SUCCESS - hashKey={}, count={}, ttl={}min",
                hashKey, hashData.size(), POST_CACHE_TTL.toMinutes());
    }

    /**
     * <h3>단일 캐시 삭제 (HDEL)</h3>
     * <p>모든 캐시 유형의 Hash에서 특정 postId 필드를 삭제합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePostFromCache(Long postId) {
        if (postId == null) {
            return;
        }

        String field = postId.toString();

        for (PostCacheFlag type : PostCacheFlag.values()) {
            String hashKey = getSimplePostHashKey(type);
            redisTemplate.opsForHash().delete(hashKey, field);
        }

        log.debug("[CACHE_DELETE] postId={}, allTypes", postId);
    }

    /**
     * <h3>특정 타입의 단일 캐시 삭제 (HDEL)</h3>
     *
     * @param type   캐시 유형
     * @param postId 제거할 게시글 ID
     */
    public void removePostFromCache(PostCacheFlag type, Long postId) {
        if (postId == null) {
            return;
        }

        String hashKey = getSimplePostHashKey(type);
        String field = postId.toString();

        redisTemplate.opsForHash().delete(hashKey, field);

        log.debug("[CACHE_DELETE] hashKey={}, field={}", hashKey, field);
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
