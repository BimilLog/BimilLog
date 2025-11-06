package jaeik.bimillog.out.redis.paper;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * <h2>롤링페이퍼 Redis 키</h2>
 * <p>롤링페이퍼 관련 모든 Redis 키 정의, 스코어링 상수 및 Lua 스크립트를 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public final class RedisPaperKeys {

    // ===================== 1. KEYS (Redis 키) =====================

    /**
     * 실시간 인기 롤링페이퍼 점수 Sorted Set 키
     * <p>Value Type: ZSet (memberId, score)</p>
     */
    public static final String REALTIME_PAPER_SCORE_KEY = "paper:realtime:score";

    // ===================== 2. SCORE CONSTANTS (점수 관련 상수) =====================

    /**
     * 실시간 인기 롤링페이퍼 점수 감쇠율 (0.97)
     * <p>주기적으로 모든 롤링페이퍼 점수에 곱해져 시간 경과에 따른 인기도 감소를 반영합니다.</p>
     */
    public static final double REALTIME_PAPER_SCORE_DECAY_RATE = 0.97;

    /**
     * 실시간 인기 롤링페이퍼 제거 임계값 (1.0)
     * <p>이 값 이하의 점수를 가진 롤링페이퍼는 실시간 인기 목록에서 제거됩니다.</p>
     */
    public static final double REALTIME_PAPER_SCORE_THRESHOLD = 1.0;

    // ===================== 3. LUA SCRIPT (Redis 스크립트) =====================

    /**
     * 실시간 인기 롤링페이퍼 점수 감쇠를 위한 Lua 스크립트
     * <p>Redis Sorted Set의 모든 롤링페이퍼 점수에 SCORE_DECAY_RATE(0.95)를 곱합니다.</p>
     */
    public static final RedisScript<Long> SCORE_DECAY_SCRIPT;

    static {
        String luaScript =
            "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES') " +
            "for i = 1, #members, 2 do " +
            "    local member = members[i] " +
            "    local score = tonumber(members[i + 1]) " +
            "    local newScore = score * tonumber(ARGV[1]) " +
            "    redis.call('ZADD', KEYS[1], newScore, member) " +
            "end " +
            "return redis.call('ZCARD', KEYS[1])";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        SCORE_DECAY_SCRIPT = script;
    }
}
