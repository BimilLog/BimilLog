package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.*;

@Repository
@RequiredArgsConstructor
public class RedisFriendInteractionAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

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
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_ERROR, e);
        }
    }

    // 상호작용 삭제
    // 회원탈퇴시 호출 파이프라이닝으로 상호작용 값 삭제
    public void deleteInteractionKey(List<Long> memberIds, Long interactionMemberId) {
        redisTemplate.executePipelined((RedisConnection connection) -> {
            connection.keyCommands().del((INTERACTION_PREFIX + interactionMemberId).getBytes());

            String deleteValue = INTERACTION_SUFFIX + interactionMemberId;

            for (Long memberId : memberIds) {
               connection.zSetCommands().zRem((INTERACTION_PREFIX + memberId).getBytes(), deleteValue.getBytes());
            }
            return null;
        });
    }
}
