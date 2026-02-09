package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>레디스 게시글 캐시 저장소 어댑터</h2>
 * <p>게시글 목록 캐시를 Hash 구조로 관리합니다.</p>
 * <p>각 캐시는 RedisKey 상수로 직접 참조합니다.</p>
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
     * 락 해제 시 자신의 UUID인 경우에만 DEL하는 Lua 스크립트.
     * TTL 만료 후 다른 인스턴스의 락을 삭제하는 Lock Overlap 문제를 방지.
     */
    private static final String SAFE_RELEASE_LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('DEL', KEYS[1]) " +
            "end " +
            "return 0";

    /**
     * 전체 게시글 목록 Hash 키 (수정/삭제 시 캐시 정리용)
     */
    private static final List<String> ALL_SIMPLE_HASH_KEYS = List.of(
            RedisKey.REALTIME_SIMPLE_KEY,
            RedisKey.WEEKLY_SIMPLE_KEY,
            RedisKey.LEGEND_SIMPLE_KEY,
            RedisKey.NOTICE_SIMPLE_KEY
    );

    /**
     * <h3>HGETALL로 Hash 전체 캐시 조회</h3>
     * <p>지정된 Hash 키에서 모든 게시글을 한 번에 조회합니다.</p>
     *
     * @param hashKey Redis Hash 키
     * @return postId를 키로, PostSimpleDetail을 값으로 하는 Map (캐시가 없으면 빈 Map)
     */
    public Map<Long, PostSimpleDetail> getAllCachedPosts(String hashKey) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
        if (entries.isEmpty()) {
            CacheMetricsLogger.miss(log, hashKey, "hgetall", "empty");
            return Collections.emptyMap();
        }

        Map<Long, PostSimpleDetail> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getValue() instanceof PostSimpleDetail post) {
                Long postId = Long.parseLong(entry.getKey().toString());
                result.put(postId, post);
            }
        }

        CacheMetricsLogger.hit(log, hashKey, "hgetall", result.size());
        return result;
    }

    /**
     * <h3>모든 featured 캐시에서 단일 게시글 삭제 (HDEL)</h3>
     * <p>주간/레전드/공지 Hash에서 특정 postId 필드를 삭제합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    public void removePostFromCache(Long postId) {
        String field = postId.toString();
        for (String hashKey : ALL_SIMPLE_HASH_KEYS) {
            redisTemplate.opsForHash().delete(hashKey, field);
        }
    }

    /**
     * <h3>특정 Hash에서 단일 캐시 삭제 (HDEL)</h3>
     *
     * @param hashKey Redis Hash 키
     * @param postId  제거할 게시글 ID
     */
    public void removePostFromCache(String hashKey, Long postId) {
        if (postId == null) {
            return;
        }

        String field = postId.toString();
        redisTemplate.opsForHash().delete(hashKey, field);

        log.debug("[CACHE_DELETE] hashKey={}, field={}", hashKey, field);
    }

    /**
     * <h3>단일 게시글 Hash 캐시 추가 (HSET)</h3>
     * <p>특정 Hash에 게시글 1건을 추가합니다.</p>
     *
     * @param hashKey Redis Hash 키
     * @param post    저장할 게시글
     */
    public void putPostToCache(String hashKey, PostSimpleDetail post) {
        if (post == null || post.getId() == null) {
            return;
        }

        String field = post.getId().toString();
        redisTemplate.opsForHash().put(hashKey, field, post);

        log.debug("[CACHE_PUT] hashKey={}, field={}", hashKey, field);
    }

    // ===================== TTL 지정 캐시 메서드 =====================

    /**
     * <h3>여러 게시글 Hash 캐시 저장 (TTL 지정)</h3>
     * <p>기존 Hash를 삭제 후 새로 저장합니다.</p>
     *
     * @param hashKey Redis Hash 키
     * @param posts   저장할 게시글 목록
     * @param ttl     캐시 TTL (null이면 영구 저장)
     */
    public void cachePostsWithTtl(String hashKey, List<PostSimpleDetail> posts, Duration ttl) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        log.info("[CACHE_WRITE] START - hashKey={}, count={}, ttl={}",
                hashKey, posts.size(), ttl);

        // Map<String, PostSimpleDetail>로 변환
        Map<String, PostSimpleDetail> hashData = posts.stream()
                .filter(post -> post != null && post.getId() != null)
                .collect(Collectors.toMap(
                        p -> p.getId().toString(),
                        p -> p
                ));

        // 임시 키에 HMSET → RENAME으로 원자적 교체 (빈 상태 노출 방지)
        String tmpKey = hashKey + ":tmp";
        redisTemplate.delete(tmpKey);
        redisTemplate.opsForHash().putAll(tmpKey, hashData);
        if (ttl != null) {
            redisTemplate.expire(tmpKey, ttl);
        }
        redisTemplate.rename(tmpKey, hashKey);

        log.info("[CACHE_WRITE] SUCCESS - hashKey={}, count={}, ttl={}",
                hashKey, hashData.size(), ttl != null ? ttl : "PERMANENT");
    }

    /**
     * <h3>HGETALL로 Hash 전체 캐시 조회 (List 반환)</h3>
     * <p>지정된 Hash에서 모든 게시글을 조회하여 ID 역순 List로 반환합니다.</p>
     *
     * @param hashKey Redis Hash 키
     * @return PostSimpleDetail 리스트 (캐시가 없으면 빈 리스트)
     */
    public List<PostSimpleDetail> getAllCachedPostsList(String hashKey) {
        Map<Long, PostSimpleDetail> cachedPosts = getAllCachedPosts(hashKey);
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
     * <h3>스케줄러 분산 락 획득 시도 (SET NX + UUID)</h3>
     * <p>다중 인스턴스 환경에서 하나의 스케줄러만 캐시를 갱신하도록 보장합니다.</p>
     * <p>UUID를 락 값으로 저장하여 해제 시 자신의 락만 삭제할 수 있도록 합니다.</p>
     *
     * @return 락 획득 성공 시 UUID, 실패 시 null
     */
    public String tryAcquireSchedulerLock() {
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                RedisKey.SCHEDULER_LOCK_KEY,
                lockValue,
                RedisKey.SCHEDULER_LOCK_TTL
        );

        return Boolean.TRUE.equals(acquired) ? lockValue : null;
    }

    /**
     * <h3>스케줄러 분산 락 안전 해제 (Lua 스크립트)</h3>
     * <p>자신의 UUID와 일치하는 경우에만 락을 삭제합니다.</p>
     * <p>TTL 만료 후 다른 인스턴스가 획득한 락을 실수로 삭제하는 문제를 방지합니다.</p>
     *
     * @param lockValue 락 획득 시 반환받은 UUID
     */
    public void releaseSchedulerLock(String lockValue) {
        if (lockValue == null) {
            return;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(SAFE_RELEASE_LOCK_SCRIPT, Long.class);
        redisTemplate.execute(script, List.of(RedisKey.SCHEDULER_LOCK_KEY), lockValue);
    }

    /**
     * <h3>조회 시 HASH 갱신 분산 락 획득 시도 (SET NX + UUID)</h3>
     * <p>HASH-ZSET ID 불일치 감지 시 비동기 갱신을 위한 락입니다.</p>
     * <p>스케줄러 락과 별도로 동작합니다.</p>
     *
     * @return 락 획득 성공 시 UUID, 실패 시 null
     */
    public String tryAcquireRealtimeRefreshLock() {
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                RedisKey.REALTIME_REFRESH_LOCK_KEY,
                lockValue,
                RedisKey.REALTIME_REFRESH_LOCK_TTL
        );
        return Boolean.TRUE.equals(acquired) ? lockValue : null;
    }

    /**
     * <h3>조회 시 HASH 갱신 분산 락 안전 해제 (Lua 스크립트)</h3>
     * <p>자신의 UUID와 일치하는 경우에만 락을 삭제합니다.</p>
     *
     * @param lockValue 락 획득 시 반환받은 UUID
     */
    public void releaseRealtimeRefreshLock(String lockValue) {
        if (lockValue == null) {
            return;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(SAFE_RELEASE_LOCK_SCRIPT, Long.class);
        redisTemplate.execute(script, List.of(RedisKey.REALTIME_REFRESH_LOCK_KEY), lockValue);
    }

}
