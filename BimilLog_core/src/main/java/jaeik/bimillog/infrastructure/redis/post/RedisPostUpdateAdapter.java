package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * <h2>게시글 카운트 버퍼 Redis 어댑터</h2>
 * <p>SET NX EX 기반 중복 조회 방지 및 조회수 버퍼링을 담당합니다.</p>
 * <ul>
 *   <li>중복 방지: String(post:view:{postId}:{viewerKey})에 SET NX EX로 24시간 중복 차단</li>
 *   <li>조회수 버퍼: HASH(post:view:counts)에 postId별 증가량 누적</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostUpdateAdapter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_PREFIX = RedisKey.VIEW_PREFIX;
    private static final Duration VIEW_TTL = Duration.ofSeconds(RedisKey.VIEW_TTL_SECONDS);
    private static final String VIEW_COUNTS_KEY = RedisKey.VIEW_COUNTS_KEY;

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
     * <h3>조회 마킹 + 조회수 증가</h3>
     * <p>SET NX EX로 중복 확인과 마킹을 원자적 1커맨드로 처리합니다.</p>
     * <p>키가 새로 생성된 경우에만 조회수 버퍼를 증가시킵니다.</p>
     *
     * @param postId    게시글 ID
     * @param viewerKey 조회자 키 (m:{memberId} 또는 ip:{clientIp})
     * @return 조회수가 증가되었으면 true, 이미 조회한 경우 false
     */
    public boolean markViewedAndIncrement(Long postId, String viewerKey) {
        String key = VIEW_PREFIX + postId + ":" + viewerKey;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", VIEW_TTL);

        if (Boolean.TRUE.equals(isNew)) {
            stringRedisTemplate.opsForHash().increment(VIEW_COUNTS_KEY, postId.toString(), 1L);
            return true;
        }
        return false;
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
        List<Object> result = stringRedisTemplate.execute(script, List.of(VIEW_COUNTS_KEY));

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
