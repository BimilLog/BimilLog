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
public class RedisFriendshipRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // 레디스 친구관계 테이블에 친구를 증가시킨다.
    public void addFriend(Long memberId, Long friendId) {
        String key1 = FRIEND_SHIP_PREFIX + memberId;
        String key2 = FRIEND_SHIP_PREFIX + friendId;

        try {
            redisTemplate.opsForSet().add(key1, friendId);
            redisTemplate.opsForSet().add(key2, memberId);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_WRITE_ERROR, e);
        }
    }

    // 레디스 친구관계 테이블에서 서로를 삭제한다.
    // 친구삭제 상황에서 발생한다.
    public void deleteFriend(Long memberId, Long friendId) {
        String key1 = FRIEND_SHIP_PREFIX + memberId;
        String key2 = FRIEND_SHIP_PREFIX + friendId;

        try {
            redisTemplate.opsForSet().remove(key1, friendId);
            redisTemplate.opsForSet().remove(key2, memberId);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_DELETE_ERROR, e);
        }

    }

    // 레디스 친구관계 테이블에서 탈퇴한 회원을 삭제한다.
    // 회원탈퇴 상황에서 발생한다.
    // 파이프라이닝으로 탈퇴한 본인과 탈퇴한 멤버를 찾아 삭제한다.
    public void deleteWithdrawFriend(List<Long> memberIds, Long withdrawFriendId) {
        byte[] withdrawMemberIdBytes = String.valueOf(withdrawFriendId).getBytes();

        try {
            redisTemplate.executePipelined((RedisConnection connection) -> {
                connection.keyCommands().del((FRIEND_SHIP_PREFIX + withdrawFriendId).getBytes());

                for (Long memberId : memberIds) {
                    byte[] memberKey = (FRIEND_SHIP_PREFIX + memberId).getBytes();
                    connection.setCommands().sRem(memberKey, withdrawMemberIdBytes);
                }
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_DELETE_ERROR, e);
        }
    }
}
