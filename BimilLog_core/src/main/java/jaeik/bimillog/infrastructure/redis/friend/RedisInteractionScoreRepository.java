package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.*;

@Repository
@RequiredArgsConstructor
public class RedisInteractionScoreRepository {

    private final RedisTemplate<String, Object> redisTemplate;

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
                    connection.zSetCommands().zScore(keyBytes, member.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });

            for (int i = 0; i < targetIdList.size(); i++) {
                Object scoreObj = results.get(i);
                if (scoreObj != null) {
                    Double score = Double.valueOf(scoreObj.toString());
                    resultMap.put(targetIdList.get(i), score);
                }
            }

            return resultMap;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_DELETE_ERROR, e);
        }
    }

    // 상호작용 점수 추가
    // ZSCORE로 기존 점수가 10점이상인지 확인 후 아닌 경우에만 작동
    // Lua 스크립트로 원자적 진행
    public void addInteractionScore(Long memberId, Long interactionMemberId) {
        String key1 = INTERACTION_PREFIX + memberId;
        String key2 = INTERACTION_PREFIX + interactionMemberId;
        String member1 = INTERACTION_SUFFIX + interactionMemberId;
        String member2 = INTERACTION_SUFFIX + memberId;
        try {
            redisTemplate.execute(
                    INTERACTION_SCORE_ADD_SCRIPT,
                    List.of(key1, key2),
                    member1,
                    member2,
                    INTERACTION_SCORE_DEFAULT.toString(),
                    INTERACTION_SCORE_LIMIT.toString()
            );
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_WRITE_ERROR, e);
        }
    }

    // 상호작용 삭제
    // 회원탈퇴시 호출 파이프라이닝으로 상호작용 값 삭제
    public void deleteInteractionKey(List<Long> memberIds, Long interactionMemberId) {
        try {
            redisTemplate.executePipelined((RedisConnection connection) -> {
                connection.keyCommands().del((INTERACTION_PREFIX + interactionMemberId).getBytes());
                String deleteValue = INTERACTION_SUFFIX + interactionMemberId;

                for (Long memberId : memberIds) {
                    connection.zSetCommands().zRem((INTERACTION_PREFIX + memberId).getBytes(), deleteValue.getBytes());
                }
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_DELETE_ERROR, e);
        }
    }


}
