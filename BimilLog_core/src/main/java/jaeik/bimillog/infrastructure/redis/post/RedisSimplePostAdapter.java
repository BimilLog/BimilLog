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

import java.time.Duration;
import java.util.HashSet;

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

        if (ttl < PER_EXPIRY_GAP_SECONDS) {
            double randomFactor = ThreadLocalRandom.current().nextDouble();
            return ttl - (randomFactor * PER_EXPIRY_GAP_SECONDS) <= 0;
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
        redisTemplate.expire(hashKey, POST_CACHE_TTL_REALTIME);

        log.info("[CACHE_WRITE] SUCCESS - hashKey={}, count={}, ttl={}min",
                hashKey, hashData.size(), POST_CACHE_TTL_REALTIME.toMinutes());
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

    // ===================== Tier2 제거 후 추가된 메서드들 =====================

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

    /**
     * <h3>인기글 여부 확인</h3>
     * <p>특정 postId가 Tier1 Hash(주간/레전드/공지)에 존재하는지 확인합니다.</p>
     * <p>실시간은 별도의 Score 저장소에서 관리되므로 제외됩니다.</p>
     *
     * @param postId 확인할 게시글 ID
     * @return 인기글이면 true
     */
    public boolean isPopularPost(Long postId) {
        if (postId == null) {
            return false;
        }

        String field = postId.toString();

        // NOTICE: Hash에서 확인
        if (Boolean.TRUE.equals(redisTemplate.opsForHash()
                .hasKey(getSimplePostHashKey(PostCacheFlag.NOTICE), field))) {
            return true;
        }

        // WEEKLY: Hash에서 확인
        if (Boolean.TRUE.equals(redisTemplate.opsForHash()
                .hasKey(getSimplePostHashKey(PostCacheFlag.WEEKLY), field))) {
            return true;
        }

        // LEGEND: Hash에서 확인
        return Boolean.TRUE.equals(redisTemplate.opsForHash()
                .hasKey(getSimplePostHashKey(PostCacheFlag.LEGEND), field));
    }

    /**
     * <h3>단일 게시글 캐시 추가</h3>
     * <p>특정 타입의 Hash에 게시글을 추가합니다.</p>
     * <p>공지사항 설정 시 사용됩니다.</p>
     *
     * @param type   캐시 유형
     * @param postId 추가할 게시글 ID
     * @param post   추가할 게시글 정보
     */
    public void addPostToCache(PostCacheFlag type, Long postId, PostSimpleDetail post) {
        if (postId == null || post == null) {
            return;
        }

        String hashKey = getSimplePostHashKey(type);
        String field = postId.toString();

        redisTemplate.opsForHash().put(hashKey, field, post);
        log.info("[CACHE_ADD] hashKey={}, postId={}", hashKey, postId);
    }

    /**
     * <h3>단일 게시글 캐시 업데이트 (Write-Through)</h3>
     * <p>게시글 수정 시 해당 게시글이 존재하는 모든 캐시를 업데이트합니다.</p>
     *
     * @param postId 업데이트할 게시글 ID
     * @param post   업데이트할 게시글 정보
     */
    public void updatePostInCache(Long postId, PostSimpleDetail post) {
        if (postId == null || post == null) {
            return;
        }

        String field = postId.toString();

        for (PostCacheFlag type : PostCacheFlag.values()) {
            if (type == PostCacheFlag.REALTIME) {
                continue; // 실시간은 별도 Score 저장소에서 관리
            }

            String hashKey = getSimplePostHashKey(type);

            // 해당 Hash에 postId가 존재하면 업데이트
            if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(hashKey, field))) {
                redisTemplate.opsForHash().put(hashKey, field, post);
                log.info("[CACHE_UPDATE] hashKey={}, postId={}", hashKey, postId);
            }
        }
    }

    // ===================== 실시간 캐시 동기화 관련 메서드 =====================

    /**
     * <h3>실시간 캐시 ID 일치 여부 확인</h3>
     * <p>Tier2(ZSet)의 postId 목록과 Tier1(Hash)의 postId 목록이 일치하는지 확인합니다.</p>
     * <p>개수가 같아도 ID가 다르면 불일치로 판단합니다.</p>
     *
     * @param tier2PostIds Tier2(ZSet)에서 조회한 postId 목록
     * @param tier1PostIds Tier1(Hash)에서 조회한 postId Set
     * @return ID가 일치하면 true
     */
    public boolean isCacheIdsMatch(List<Long> tier2PostIds, Set<Long> tier1PostIds) {
        if (tier2PostIds == null || tier1PostIds == null) {
            return false;
        }

        // 개수가 다르면 불일치
        if (tier2PostIds.size() != tier1PostIds.size()) {
            return false;
        }

        // Tier2의 모든 ID가 Tier1에 존재하는지 확인
        Set<Long> tier2Set = new HashSet<>(tier2PostIds);
        return tier1PostIds.containsAll(tier2Set);
    }

    /**
     * <h3>실시간 캐시 갱신 락 획득 시도 (SET NX)</h3>
     * <p>ID 불일치 시 중복 갱신을 방지하기 위해 분산 락을 획득합니다.</p>
     * <p>락 TTL은 30초이며, 락 획득 실패 시 다른 스레드가 이미 갱신 중임을 의미합니다.</p>
     *
     * @return 락 획득 성공 시 true
     */
    public boolean tryAcquireRefreshLock() {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                REALTIME_REFRESH_LOCK_KEY,
                "1",
                REALTIME_REFRESH_LOCK_TTL
        );
        boolean result = Boolean.TRUE.equals(acquired);
        if (result) {
            log.debug("[LOCK_ACQUIRED] 실시간 캐시 갱신 락 획득");
        }
        return result;
    }

    /**
     * <h3>실시간 캐시 갱신 락 해제</h3>
     * <p>갱신 완료 후 락을 해제합니다.</p>
     */
    public void releaseRefreshLock() {
        redisTemplate.delete(REALTIME_REFRESH_LOCK_KEY);
        log.debug("[LOCK_RELEASED] 실시간 캐시 갱신 락 해제");
    }
}
