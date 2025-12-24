package jaeik.bimillog.infrastructure.redis.post;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static jaeik.bimillog.infrastructure.redis.post.RedisPostKeys.*;

@Component
@RequiredArgsConstructor
public class RedisPostUpdateAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>실시간 인기글 점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score  증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     * @author Jaeik
     * @since 2.0.0
     */
    public void incrementRealtimePopularScore(Long postId, double score) {
        redisTemplate.opsForZSet().incrementScore(REALTIME_POST_SCORE_KEY, postId.toString(), score);
    }

    /**
     * <h3>실시간 인기글 전체 점수 지수감쇠 적용</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.9를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostScheduledService 스케줄러에서 5분마다 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void applyRealtimePopularScoreDecay() {
        // 1. 모든 항목의 점수에 0.9 곱하기 (Lua 스크립트 사용)
        redisTemplate.execute(
                SCORE_DECAY_SCRIPT,
                List.of(REALTIME_POST_SCORE_KEY),
                REALTIME_POST_SCORE_DECAY_RATE
        );

        // 2. 임계값(1점) 이하의 게시글 제거
        redisTemplate.opsForZSet().removeRangeByScore(REALTIME_POST_SCORE_KEY, 0, REALTIME_POST_SCORE_THRESHOLD);
    }

    // 락 해제 시 소유권을 확인하고 삭제하는 Lua Script
    private static final String RELEASE_LOCK_LUA_SCRIPT = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """;

    // 락 획득 시 사용될 고유 ID를 ThreadLocal에 저장합니다.
    // 이렇게 해야 비즈니스 로직(asyncRefreshCache) 내에서 락 해제 시 고유 ID를 사용할 수 있습니다.
    private final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();


    private String getCacheRefreshLockKey(PostCacheFlag type) {
        return "lock:cache:refresh:" + type.name();
    }

    /**
     * <h3>캐시 갱신 분산 락 획득</h3>
     * 락 획득 시 고유 ID를 값으로 저장하고, 획득에 성공하면 ThreadLocal에 해당 ID를 저장합니다.
     * SET key value NX PX timeout 명령어를 사용하여 획득과 만료 설정을 원자적으로 처리합니다.
     */
    public Boolean acquireCacheRefreshLock(PostCacheFlag type, Duration timeout) {
        String lockKey = getCacheRefreshLockKey(type);
        // 락의 값으로 사용할 고유 ID 생성
        String uniqueId = UUID.randomUUID().toString();

        // SETNX + EX 원자적 실행 (Spring Data Redis의 setIfAbsent는 이를 보장합니다.)
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                uniqueId,
                timeout
        );

        if (Boolean.TRUE.equals(acquired)) {
            // 락 획득 성공 시, 현재 스레드에 고유 ID 저장
            lockValueHolder.set(uniqueId);
            return true;
        }
        return false;
    }

    /**
     * <h3>캐시 갱신 분산 락 해제 (안전하게 수정됨)</h3>
     * Lua Script를 사용하여 저장된 값(획득 시 사용된 고유 ID)을 확인하고 락을 해제합니다.
     */
    public void releaseCacheRefreshLock(PostCacheFlag type) {
        String lockKey = getCacheRefreshLockKey(type);
        String expectedValue = lockValueHolder.get();

        // 락 획득에 실패했거나, ThreadLocal에 값이 없으면 해제 시도 자체를 건너뜁니다.
        if (expectedValue == null) {
            // 이미 락이 만료되었거나, acquireCacheRefreshLock에서 실패한 경우일 수 있습니다.
            return;
        }

        try {
            // Lua Script를 실행하여 값 확인 및 삭제를 원자적으로 처리
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(RELEASE_LOCK_LUA_SCRIPT, Long.class);

            redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(lockKey), // KEYS[1]
                    expectedValue // ARGV[1]
            );

        } finally {
            // 락 해제 후 ThreadLocal 값 제거 (메모리 누수 방지)
            lockValueHolder.remove();
        }
    }
}
