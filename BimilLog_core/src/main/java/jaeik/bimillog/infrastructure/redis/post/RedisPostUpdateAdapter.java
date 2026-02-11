package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <h2>게시글 카운트 버퍼 Redis 어댑터</h2>
 * <p>Redis SET 기반 중복 조회 방지 및 조회수/좋아요/댓글수 버퍼링을 담당합니다.</p>
 * <ul>
 *   <li>중복 방지: SET(post:view:{postId})에 viewerKey(m:{memberId} 또는 ip:{ip}) 저장</li>
 *   <li>조회수 버퍼: HASH(post:view:counts)에 postId별 증가량 누적</li>
 *   <li>좋아요 버퍼: HASH(post:like:counts)에 postId별 증감량 누적</li>
 *   <li>댓글수 버퍼: HASH(post:comment:counts)에 postId별 증감량 누적</li>
 * </ul>
 *
 * @author Jaeik
 * @version 3.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostUpdateAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_PREFIX = RedisKey.VIEW_PREFIX;
    private static final long VIEW_TTL_SECONDS = RedisKey.VIEW_TTL_SECONDS;
    private static final String VIEW_COUNTS_KEY = RedisKey.VIEW_COUNTS_KEY;
    private static final String LIKE_COUNTS_KEY = RedisKey.LIKE_COUNTS_KEY;
    private static final String COMMENT_COUNTS_KEY = RedisKey.COMMENT_COUNTS_KEY;

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
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, viewerKey);
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
        stringRedisTemplate.opsForSet().add(key, viewerKey);
        stringRedisTemplate.expire(key, VIEW_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * <h3>조회수 증가 (버퍼)</h3>
     * <p>Redis Hash에 해당 게시글의 조회수를 1 증가시킵니다.</p>
     *
     * @param postId 게시글 ID
     */
    public void incrementViewCount(Long postId) {
        stringRedisTemplate.opsForHash().increment(VIEW_COUNTS_KEY, postId.toString(), 1L);
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
        Long result = stringRedisTemplate.execute(
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
        return getAndClearCounts(VIEW_COUNTS_KEY);
    }

    // ==================== 좋아요 카운트 버퍼 ====================

    /**
     * <h3>좋아요 카운트 버퍼 증감</h3>
     * <p>Redis Hash에 해당 게시글의 좋아요 증감량을 누적시킵니다.</p>
     *
     * @param postId 게시글 ID
     * @param delta  증감량 (양수: 좋아요, 음수: 취소)
     */
    public void incrementLikeBuffer(Long postId, long delta) {
        stringRedisTemplate.opsForHash().increment(LIKE_COUNTS_KEY, postId.toString(), delta);
    }

    /**
     * <h3>좋아요 버퍼 조회 및 초기화 (원자적)</h3>
     *
     * @return postId → 증감량 맵
     */
    public Map<Long, Long> getAndClearLikeCounts() {
        return getAndClearCounts(LIKE_COUNTS_KEY);
    }

    // ==================== 댓글수 카운트 버퍼 ====================

    /**
     * <h3>댓글수 카운트 버퍼 증감</h3>
     * <p>Redis Hash에 해당 게시글의 댓글수 증감량을 누적시킵니다.</p>
     *
     * @param postId 게시글 ID
     * @param delta  증감량 (양수: 작성, 음수: 삭제)
     */
    public void incrementCommentBuffer(Long postId, long delta) {
        stringRedisTemplate.opsForHash().increment(COMMENT_COUNTS_KEY, postId.toString(), delta);
    }

    /**
     * <h3>댓글수 버퍼 조회 및 초기화 (원자적)</h3>
     *
     * @return postId → 증감량 맵
     */
    public Map<Long, Long> getAndClearCommentCounts() {
        return getAndClearCounts(COMMENT_COUNTS_KEY);
    }

    // ==================== 공통 버퍼 읽기+삭제 ====================

    @SuppressWarnings("unchecked")
    private Map<Long, Long> getAndClearCounts(String countsKey) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>(GET_AND_CLEAR_VIEW_COUNTS_SCRIPT, List.class);
        List<Object> result = stringRedisTemplate.execute(script, List.of(countsKey));

        if (result == null || result.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (int i = 0; i + 1 < result.size(); i += 2) {
            Object keyObj = result.get(i);
            Object valueObj = result.get(i + 1);
            if (keyObj == null || valueObj == null) {
                continue;
            }
            counts.put(Long.parseLong(keyObj.toString()), Long.parseLong(valueObj.toString()));
        }
        return counts;
    }
}
