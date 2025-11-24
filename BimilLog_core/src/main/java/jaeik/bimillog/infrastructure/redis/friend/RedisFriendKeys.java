package jaeik.bimillog.infrastructure.redis.friend;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

public class RedisFriendKeys {

    // 친구 관계 테이블 (Set) 키
    public static final String FRIEND_SHIP_PREFIX = "friend:";

    // 상호 작용 점수 테이블(Sorted Set) 키 접두사
    public static final String INTERACTION_PREFIX = "interaction:";

    // 상호 작용 점수 테이블(Sorted Set) 값 접두사
    public static final String INTERACTION_SUFFIX = "member:";

    // 상호 작용 점수 증가 가능 최대값 (최대값은 10점)
    public static final Double INTERACTION_SCORE_LIMIT = 9.5;

    // 상호 작용 점수 증가 기본 값
    public static final Double INTERACTION_SCORE_DEFAULT= 0.5;

    // 상호 작용 점수 증가 Lua Script
    public static final RedisScript<Long> INTERACTION_SCORE_ADD_SCRIPT;
    static {
        String luaScript = """
            local key1 = KEYS[1]      -- Member A의 Sorted Set 키 (interaction:A)
            local key2 = KEYS[2]      -- Member B의 Sorted Set 키 (interaction:B)
            local member1 = ARGV[1]   -- Key1에 저장될 Member B의 ID (member:B)
            local member2 = ARGV[2]   -- Key2에 저장될 Member A의 ID (member:A)
            local increment = tonumber(ARGV[3]) -- 증가시킬 점수 (0.5)
            local maxScore = tonumber(ARGV[4])  -- 증가 가능 최대값 (9.5)

            -- 1) 첫 번째 Sorted Set (key1) 업데이트
            -- 현재 점수가 증가 가능 최대값(9.5) 이하인 경우에만 작업 수행
            local current1 = redis.call('ZSCORE', key1, member1)
            if not current1 or tonumber(current1) <= maxScore then
                -- 점수 증가
                local newScore1 = redis.call('ZINCRBY', key1, increment, member1)
            end

            -- 2) 두 번째 Sorted Set (key2) 업데이트
            -- 현재 점수가 증가 가능 최대값(9.5) 이하인 경우에만 작업 수행
            local current2 = redis.call('ZSCORE', key2, member2)
            if not current2 or tonumber(current2) <= maxScore then
                -- 점수 증가
                local newScore2 = redis.call('ZINCRBY', key2, increment, member2)
            end
            
            -- 성공적으로 완료되면 1 반환
            return 1
            """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        INTERACTION_SCORE_ADD_SCRIPT = script;
    }
}
