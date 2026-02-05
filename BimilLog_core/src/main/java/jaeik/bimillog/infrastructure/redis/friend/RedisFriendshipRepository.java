package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.FRIEND_SHIP_PREFIX;

/**
 * <h2>친구 관계 Redis 캐시 저장소</h2>
 * <p>Redis를 사용하여 친구 관계를 캐싱합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Repository
@RequiredArgsConstructor
public class RedisFriendshipRepository {
    private final RedisTemplate<String, Long> redisTemplate;
    private static final int PIPELINE_BATCH_SIZE = 500;

    /**
     * <h3>특정 회원의 1촌 친구 목록 조회 (랜덤 추출)</h3>
     */
    public Set<Long> getFriends(Long memberId, int count) {
        String key = FRIEND_SHIP_PREFIX + memberId;
        return redisTemplate.opsForSet().distinctRandomMembers(key, count);
    }

    /**
     * <h3>여러 회원의 친구 목록을 파이프라인으로 일괄 조회</h3>
     *
     * @param memberIdList 조회할 회원 ID 목록 (순서 유지)
     * @param sampleSize   각 회원당 랜덤 추출할 친구 수
     * @return 파이프라인 결과 (memberIdList 순서와 동일)
     */
    public List<Object> getFriendsBatch(List<Long> memberIdList, int sampleSize) {
        try {
            return redisTemplate.executePipelined((RedisConnection connection) -> {
                for (Long memberId : memberIdList) {
                    String key = FRIEND_SHIP_PREFIX + memberId;
                    connection.setCommands().sRandMember(key.getBytes(StandardCharsets.UTF_8), sampleSize);
                }
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_QUERY_ERROR, e);
        }
    }

    /**
     * <h3>레디스 친구 증가</h3>
     */
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

    /**
     * <h3>레디스 친구 삭제</h3>
     */
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

    /**
     * <h3>회원 탈퇴상항 친구 삭제</h3>
     * <p>회원탈퇴 상황에서 발생한다.</p>
     * <p>레디스 친구관계 테이블에서 탈퇴한 회원을 삭제한다.</p>
     * <p>탈퇴 회원의 친구 목록을 기반으로 타겟 키만 정리한다.</p>
     */
    public void deleteWithdrawFriendTargeted(Long withdrawFriendId) {
        try {
            Set<Long> friendIds = getFriends(withdrawFriendId, PIPELINE_BATCH_SIZE);
            if (friendIds != null && !friendIds.isEmpty()) {
                byte[] withdrawMemberIdBytes = String.valueOf(withdrawFriendId).getBytes(StandardCharsets.UTF_8);
                List<Long> friendIdList = new ArrayList<>(friendIds);
                for (int i = 0; i < friendIdList.size(); i += PIPELINE_BATCH_SIZE) {
                    int end = Math.min(i + PIPELINE_BATCH_SIZE, friendIdList.size());
                    List<Long> batch = friendIdList.subList(i, end);
                    redisTemplate.executePipelined((RedisConnection connection) -> {
                        for (Long friendId : batch) {
                            String friendKey = FRIEND_SHIP_PREFIX + friendId;
                            connection.setCommands().sRem(friendKey.getBytes(StandardCharsets.UTF_8), withdrawMemberIdBytes);
                        }
                        return null;
                    });
                }
            }

            String withdrawKey = FRIEND_SHIP_PREFIX + withdrawFriendId;
            redisTemplate.delete(withdrawKey);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_DELETE_ERROR, e);
        }
    }
}
