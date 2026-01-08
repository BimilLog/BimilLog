package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.*;
import static org.springframework.data.redis.core.ScanOptions.scanOptions;

/**
 * <h2>상호작용 점수 Redis 캐시 저장소</h2>
 * <p>Redis를 사용하여 회원 간 상호작용 점수를 캐싱합니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Repository
@RequiredArgsConstructor
public class RedisInteractionScoreRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // 상호 작용 점수 증가 Lua Script
    public static final RedisScript<Long> INTERACTION_SCORE_ADD_SCRIPT;
    static {
        String luaScript = """
            local key1 = KEYS[1]
            local key2 = KEYS[2]
            
            -- Argument는 String으로 넘어오지만, 명시적으로 tostring을 사용합니다.
            local member1 = tostring(ARGV[1]) 
            local member2 = tostring(ARGV[2]) 
            
            local increment = ARGV[3]
            local maxScore = tonumber(ARGV[4])
            
            local current1 = redis.call('HGET', key1, member1)
            if not current1 or tonumber(current1) <= maxScore then
                redis.call('HINCRBYFLOAT', key1, member1, increment)
            end

            local current2 = redis.call('HGET', key2, member2)
            if not current2 or tonumber(current2) <= maxScore then
                redis.call('HINCRBYFLOAT', key2, member2, increment)
            end
            
            return 1
        """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        INTERACTION_SCORE_ADD_SCRIPT = script;
    }

    /**
     * 후보자들의 상호작용 점수만 파이프라인으로 일괄 조회
     *
     * @param memberId 기준 회원 ID
     * @param targetIds 점수를 조회할 대상(후보자) ID 목록
     * @return Map<대상ID, 점수>
     */
    public Map<Long, Double> getInteractionScoresBatch(Long memberId, Set<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return new HashMap<>();
        }

        String key = INTERACTION_PREFIX + memberId;
        List<Long> targetIdList = new ArrayList<>(targetIds);
        Map<Long, Double> resultMap = new HashMap<>();

        try {
            List<Object> results = redisTemplate.executePipelined((RedisConnection connection) -> {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                for (Long targetId : targetIdList) {
                    String member = INTERACTION_SUFFIX + targetId;
                    connection.hashCommands().hGet(keyBytes, member.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });

            for (int i = 0; i < targetIdList.size(); i++) {
                Object scoreObj = results.get(i);
                if (scoreObj != null) {
                    String scoreValue;
                    if (scoreObj instanceof byte[] bytes) {
                        scoreValue = new String(bytes, StandardCharsets.UTF_8);
                    } else {
                        scoreValue = scoreObj.toString();
                    }
                    Double score = Double.valueOf(scoreValue);
                    resultMap.put(targetIdList.get(i), score);
                }
            }

            return resultMap;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_QUERY_ERROR, e);
        }
    }

    /**
     * 상호작용 점수 추가
     * HGET으로 기존 점수가 10점이상인지 확인 후 아닌 경우에만 작동
     * Lua 스크립트로 원자적 진행
     */
    public void addInteractionScore(Long memberId, Long interactionMemberId) {
        String key1 = INTERACTION_PREFIX + memberId;
        String key2 = INTERACTION_PREFIX + interactionMemberId;
        String member1 = INTERACTION_SUFFIX + interactionMemberId;
        String member2 = INTERACTION_SUFFIX + memberId;
        try {
            // 일반 문자열로 직렬화되도록 보장합니다.
            stringRedisTemplate.execute(
                    INTERACTION_SCORE_ADD_SCRIPT,
                    List.of(key1, key2),
                    member1,
                    member2,
                    String.valueOf(INTERACTION_SCORE_DEFAULT),
                    String.valueOf(INTERACTION_SCORE_LIMIT)
            );

        } catch (Exception e) {
            // Exception은 여전히 RedisSystemException일 수 있으므로 CustomException으로 감쌉니다.
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_WRITE_ERROR, e);
        }
    }

    // 상호작용 삭제
    // 회원탈퇴시 호출 SCAN 패턴 매칭으로 상호작용 값 삭제
    public void deleteInteractionKeyByWithdraw(Long withdrawMemberId) {
        try {
            // 1. 탈퇴 회원의 상호작용 테이블 삭제
            String withdrawKey = INTERACTION_PREFIX + withdrawMemberId;
            redisTemplate.delete(withdrawKey);

            // 2. SCAN으로 모든 interaction 키 조회 후, 탈퇴 회원 데이터 제거
            String pattern = INTERACTION_PREFIX + "*";
            String deleteMember = INTERACTION_SUFFIX + withdrawMemberId;

            redisTemplate.execute((RedisConnection connection) -> {
                byte[] deleteMemberBytes = deleteMember.getBytes(StandardCharsets.UTF_8);

                // SCAN으로 패턴 매칭 키 조회
                connection.keyCommands().scan(scanOptions()
                        .match(pattern)
                        .count(100)
                        .build())
                        .forEachRemaining(key -> {
                            // 각 키에서 탈퇴 회원 제거
                            connection.hashCommands().hDel(key, deleteMemberBytes);
                        });
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_DELETE_ERROR, e);
        }
    }


}
