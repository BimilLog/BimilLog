package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

import java.time.Duration;

/**
 * <h2>레디스 게시글 캐시 저장소 어댑터</h2>
 * <p>게시글 목록 캐시를 타입별 Hash 구조로 관리합니다.</p>
 * <p>각 타입별로 하나의 Hash 키를 사용하여 N+1 문제를 해결합니다.</p>
 * <p>키 형식: post:{type}:simple (Hash, field=postId, value=PostSimpleDetail)</p>
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

    // ===================== TTL 지정 캐시 메서드 =====================

    /**
     * <h3>여러 게시글 Hash 캐시 저장 (TTL 지정)</h3>
     * <p>주간/레전드 인기글에 TTL 1일을 적용하기 위해 사용합니다.</p>
     * <p>기존 Hash를 삭제 후 새로 저장합니다.</p>
     *
     * @param type  캐시 유형
     * @param posts 저장할 게시글 목록
     * @param ttl   캐시 TTL
     */
    public void cachePostsWithTtl(PostCacheFlag type, List<PostSimpleDetail> posts, Duration ttl) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        String hashKey = getSimplePostHashKey(type);

        log.info("[CACHE_WRITE] START - hashKey={}, count={}, ttl={}",
                hashKey, posts.size(), ttl);

        // 기존 캐시 삭제 (전체 교체)
        redisTemplate.delete(hashKey);

        // Map<String, PostSimpleDetail>로 변환
        Map<String, PostSimpleDetail> hashData = posts.stream()
                .filter(post -> post != null && post.getId() != null)
                .collect(Collectors.toMap(
                        p -> p.getId().toString(),
                        p -> p
                ));

        // HMSET 1회로 저장
        redisTemplate.opsForHash().putAll(hashKey, hashData);

        // TTL 적용 (null이면 TTL 없음 = 영구 저장)
        if (ttl != null) {
            redisTemplate.expire(hashKey, ttl);
        }

        log.info("[CACHE_WRITE] SUCCESS - hashKey={}, count={}, ttl={}",
                hashKey, hashData.size(), ttl != null ? ttl : "PERMANENT");
    }

    /**
     * <h3>HGETALL로 Hash 전체 캐시 조회 (List 반환)</h3>
     * <p>타입별 Hash에서 모든 게시글을 조회하여 List로 반환합니다.</p>
     * <p>주간/레전드/공지는 ID 순으로 정렬되어 반환됩니다.</p>
     *
     * @param type 캐시 유형
     * @return PostSimpleDetail 리스트 (캐시가 없으면 빈 리스트)
     */
    public List<PostSimpleDetail> getAllCachedPostsList(PostCacheFlag type) {
        Map<Long, PostSimpleDetail> cachedPosts = getAllCachedPosts(type);
        if (cachedPosts.isEmpty()) {
            return Collections.emptyList();
        }

        // ID 역순 정렬 (최신 글이 먼저)
        return cachedPosts.values().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
    }

    // ===================== 스케줄러 분산 락 관련 메서드 =====================

    /**
     * <h3>스케줄러 분산 락 획득 시도 (SET NX)</h3>
     * <p>다중 인스턴스 환경에서 하나의 스케줄러만 캐시를 갱신하도록 보장합니다.</p>
     *
     * @return 락 획득 성공 시 true
     */
    public boolean tryAcquireSchedulerLock() {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                SCHEDULER_LOCK_KEY,
                "1",
                SCHEDULER_LOCK_TTL
        );
        boolean result = Boolean.TRUE.equals(acquired);
        if (result) {
            log.debug("[SCHEDULER_LOCK_ACQUIRED] 스케줄러 분산 락 획득");
        }
        return result;
    }

    /**
     * <h3>스케줄러 분산 락 해제</h3>
     * <p>스케줄러 실행 완료 후 락을 해제합니다.</p>
     */
    public void releaseSchedulerLock() {
        redisTemplate.delete(SCHEDULER_LOCK_KEY);
        log.debug("[SCHEDULER_LOCK_RELEASED] 스케줄러 분산 락 해제");
    }

    /**
     * <h3>조회 시 HASH 갱신 분산 락 획득 시도 (SET NX)</h3>
     * <p>HASH-ZSET ID 불일치 감지 시 비동기 갱신을 위한 락입니다.</p>
     * <p>스케줄러 락과 별도로 동작합니다.</p>
     *
     * @return 락 획득 성공 시 true
     */
    public boolean tryAcquireRealtimeRefreshLock() {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                REALTIME_REFRESH_LOCK_KEY,
                "1",
                REALTIME_REFRESH_LOCK_TTL
        );
        boolean result = Boolean.TRUE.equals(acquired);
        if (result) {
            log.debug("[REALTIME_REFRESH_LOCK_ACQUIRED] 조회 갱신 분산 락 획득");
        }
        return result;
    }

    /**
     * <h3>조회 시 HASH 갱신 분산 락 해제</h3>
     */
    public void releaseRealtimeRefreshLock() {
        redisTemplate.delete(REALTIME_REFRESH_LOCK_KEY);
        log.debug("[REALTIME_REFRESH_LOCK_RELEASED] 조회 갱신 분산 락 해제");
    }

    /**
     * <h3>Hash 키 전체 삭제</h3>
     * <p>특정 타입의 Hash 캐시를 전체 삭제합니다.</p>
     *
     * @param type 삭제할 캐시 유형
     */
    public void deleteHash(PostCacheFlag type) {
        String hashKey = getSimplePostHashKey(type);
        redisTemplate.delete(hashKey);
        log.info("[CACHE_DELETE_HASH] hashKey={}", hashKey);
    }

}
