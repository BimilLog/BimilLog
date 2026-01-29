package jaeik.bimillog.infrastructure.redis.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

/**
 * <h2>게시글 조회 Redis 어댑터</h2>
 * <p>Redis SET 기반 중복 조회 방지 및 조회수 버퍼링을 담당합니다.</p>
 * <ul>
 *   <li>중복 방지: SET(post:view:{postId})에 viewerKey(m:{memberId} 또는 ip:{ip}) 저장</li>
 *   <li>조회수 버퍼: HASH(post:view:counts)에 postId별 증가량 누적</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostViewAdapter {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VIEW_COUNTS_FLUSH_KEY = VIEW_COUNTS_KEY + ":flush";

    /**
     * <h3>조회 여부 확인</h3>
     * <p>Redis SET에서 해당 게시글에 대한 조회 이력이 있는지 확인합니다.</p>
     *
     * @param postId    게시글 ID
     * @param viewerKey 조회자 키 (m:{memberId} 또는 ip:{clientIp})
     * @return 이미 조회한 경우 true
     */
    public boolean hasViewed(Long postId, String viewerKey) {
        String key = VIEW_PREFIX + postId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, viewerKey);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * <h3>조회 마킹</h3>
     * <p>Redis SET에 조회 이력을 저장하고 24시간 TTL을 설정합니다.</p>
     *
     * @param postId    게시글 ID
     * @param viewerKey 조회자 키
     */
    public void markViewed(Long postId, String viewerKey) {
        String key = VIEW_PREFIX + postId;
        redisTemplate.opsForSet().add(key, viewerKey);
        redisTemplate.expire(key, VIEW_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * <h3>조회수 증가 (버퍼)</h3>
     * <p>Redis Hash에 해당 게시글의 조회수를 1 증가시킵니다.</p>
     *
     * @param postId 게시글 ID
     */
    public void incrementViewCount(Long postId) {
        redisTemplate.opsForHash().increment(VIEW_COUNTS_KEY, postId.toString(), 1L);
    }

    /**
     * <h3>조회수 버퍼 조회 및 초기화</h3>
     * <p>RENAME 패턴으로 버퍼를 임시 키로 옮긴 후 안전하게 읽습니다.</p>
     * <p>RENAME은 원자적이므로 키 스왑 시점에 유입되는 HINCRBY는 새 키에 쌓입니다.</p>
     *
     * @return postId → 증가량 맵 (비어있으면 빈 맵)
     */
    public Map<Long, Long> getAndClearViewCounts() {
        Boolean renamed = redisTemplate.renameIfAbsent(VIEW_COUNTS_KEY, VIEW_COUNTS_FLUSH_KEY);
        if (!renamed) {
            return Collections.emptyMap();
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(VIEW_COUNTS_FLUSH_KEY);
        redisTemplate.delete(VIEW_COUNTS_FLUSH_KEY);

        Map<Long, Long> counts = new HashMap<>();
        entries.forEach((k, v) -> counts.put(Long.parseLong(k.toString()), Long.parseLong(v.toString())));
        return counts;
    }

    /**
     * <h3>조회수 플러시 분산 락 획득</h3>
     *
     * @return 락 획득 성공 시 true
     */
    public boolean tryAcquireFlushLock() {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                VIEW_FLUSH_LOCK_KEY,
                "1",
                VIEW_FLUSH_LOCK_TTL
        );
        return Boolean.TRUE.equals(acquired);
    }
}
