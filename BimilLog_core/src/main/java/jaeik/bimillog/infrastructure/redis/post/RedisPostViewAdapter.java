package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private static final String VIEW_PREFIX = RedisKey.VIEW_PREFIX;
    private static final long VIEW_TTL_SECONDS = RedisKey.VIEW_TTL_SECONDS;
    private static final String VIEW_COUNTS_KEY = RedisKey.VIEW_COUNTS_KEY;

    /**
     * 조회 마킹 + 조회수 증가를 원자적으로 수행하는 Lua 스크립트.
     * SISMEMBER → SADD → EXPIRE → HINCRBY를 단일 트랜잭션으로 처리하여 레이스 컨디션 방지.
     */
    private static final String MARK_VIEWED_AND_INCREMENT_SCRIPT =
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
            "    return 0 " +
            "end " +
            "redis.call('SADD', KEYS[1], ARGV[1]) " +
            "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3])) " +
            "redis.call('HINCRBY', KEYS[2], ARGV[2], 1) " +
            "return 1";

    /**
     * 조회수 버퍼를 원자적으로 읽고 삭제하는 Lua 스크립트.
     * EXISTS → HGETALL → DEL을 단일 트랜잭션으로 처리하여 RENAME 갭 문제 해결.
     */
    private static final String GET_AND_CLEAR_VIEW_COUNTS_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "    return nil " +
            "end " +
            "local entries = redis.call('HGETALL', KEYS[1]) " +
            "redis.call('DEL', KEYS[1]) " +
            "return entries";

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
     * <h3>조회 마킹 + 조회수 증가 (원자적)</h3>
     * <p>Lua 스크립트로 SISMEMBER → SADD → EXPIRE → HINCRBY를 원자적으로 처리합니다.</p>
     * <p>동시 요청 시 중복 조회수 증가를 방지합니다.</p>
     *
     * @param postId    게시글 ID
     * @param viewerKey 조회자 키 (m:{memberId} 또는 ip:{clientIp})
     * @return 조회수가 증가되었으면 true, 이미 조회한 경우 false
     */
    public boolean markViewedAndIncrement(Long postId, String viewerKey) {
        String viewSetKey = VIEW_PREFIX + postId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(MARK_VIEWED_AND_INCREMENT_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                List.of(viewSetKey, VIEW_COUNTS_KEY),
                viewerKey, postId.toString(), String.valueOf(VIEW_TTL_SECONDS)
        );

        return result == 1L;
    }

    /**
     * <h3>조회수 버퍼 조회 및 초기화 (원자적)</h3>
     * <p>Lua 스크립트로 EXISTS → HGETALL → DEL을 원자적으로 처리합니다.</p>
     * <p>RENAME 패턴의 갭 문제와 다중 인스턴스 데이터 손실을 방지합니다.</p>
     *
     * @return postId → 증가량 맵 (비어있으면 빈 맵)
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Long> getAndClearViewCounts() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>(GET_AND_CLEAR_VIEW_COUNTS_SCRIPT, List.class);
        List<Object> result = redisTemplate.execute(script, List.of(VIEW_COUNTS_KEY));

        if (result.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (int i = 0; i < result.size(); i += 2) {
            String key = result.get(i).toString();
            String value = result.get(i + 1).toString();
            counts.put(Long.parseLong(key), Long.parseLong(value));
        }
        return counts;
    }
}
