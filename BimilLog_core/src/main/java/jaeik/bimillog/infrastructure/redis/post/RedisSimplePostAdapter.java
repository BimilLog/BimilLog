package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * <h2>레디스 분산 락 어댑터</h2>
 * <p>스케줄러 분산 락을 관리합니다.</p>
 * <p>다중 인스턴스 환경에서 하나의 스케줄러만 실행되도록 보장합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
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
}
