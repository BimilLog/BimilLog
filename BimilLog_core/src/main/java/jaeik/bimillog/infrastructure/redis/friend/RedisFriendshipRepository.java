package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    private final StringRedisTemplate stringRedisTemplate;

    private static final String FRIEND_SHIP_PREFIX = RedisKey.FRIENDSHIP_PREFIX;
    private static final int PIPELINE_BATCH_SIZE = 500;

    /**
     * <h3>특정 회원의 1촌 친구 목록 조회 (랜덤 추출)</h3>
     */
    public Set<Long> getFriendIdRandom(Long memberId, int count) {
        String key = FRIEND_SHIP_PREFIX + memberId;
        Set<String> members = stringRedisTemplate.opsForSet().distinctRandomMembers(key, count);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream().map(Long::parseLong).collect(Collectors.toSet());
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
            List<Object> rawResults = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                for (Long memberId : memberIdList) {
                    String key = FRIEND_SHIP_PREFIX + memberId;
                    connection.setCommands().sRandMember(key.getBytes(StandardCharsets.UTF_8), sampleSize);
                }
                return null;
            });

            // String → Long 변환 (호출자가 List<Long> 캐스팅 사용)
            return rawResults.stream()
                    .map(result -> {
                        if (result instanceof List<?> list) {
                            return (Object) list.stream()
                                    .map(item -> Long.parseLong(item.toString()))
                                    .toList();
                        }
                        return result;
                    })
                    .toList();
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
            stringRedisTemplate.opsForSet().add(key1, String.valueOf(friendId));
            stringRedisTemplate.opsForSet().add(key2, String.valueOf(memberId));
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
            stringRedisTemplate.opsForSet().remove(key1, String.valueOf(friendId));
            stringRedisTemplate.opsForSet().remove(key2, String.valueOf(memberId));
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
            Set<Long> friendIds = getFriendIdRandom(withdrawFriendId, PIPELINE_BATCH_SIZE);
            if (friendIds != null && !friendIds.isEmpty()) {
                byte[] withdrawMemberIdBytes = String.valueOf(withdrawFriendId).getBytes(StandardCharsets.UTF_8);
                List<Long> friendIdList = new ArrayList<>(friendIds);
                for (int i = 0; i < friendIdList.size(); i += PIPELINE_BATCH_SIZE) {
                    int end = Math.min(i + PIPELINE_BATCH_SIZE, friendIdList.size());
                    List<Long> batch = friendIdList.subList(i, end);
                    stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                        for (Long friendId : batch) {
                            String friendKey = FRIEND_SHIP_PREFIX + friendId;
                            connection.setCommands().sRem(friendKey.getBytes(StandardCharsets.UTF_8), withdrawMemberIdBytes);
                        }
                        return null;
                    });
                }
            }

            String withdrawKey = FRIEND_SHIP_PREFIX + withdrawFriendId;
            stringRedisTemplate.delete(withdrawKey);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_DELETE_ERROR, e);
        }
    }

    public void processFriendAdd(RedisConnection connection, FriendEventDlq event) {
        String key1 = FRIEND_SHIP_PREFIX + event.getMemberId();
        String key2 = FRIEND_SHIP_PREFIX + event.getTargetId();

        connection.setCommands().sAdd(
                key1.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getTargetId()).getBytes(StandardCharsets.UTF_8));
        connection.setCommands().sAdd(
                key2.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getMemberId()).getBytes(StandardCharsets.UTF_8));
    }

    public void processFriendRemove(RedisConnection connection, FriendEventDlq event) {
        String key1 = FRIEND_SHIP_PREFIX + event.getMemberId();
        String key2 = FRIEND_SHIP_PREFIX + event.getTargetId();

        connection.setCommands().sRem(
                key1.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getTargetId()).getBytes(StandardCharsets.UTF_8));
        connection.setCommands().sRem(
                key2.getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getMemberId()).getBytes(StandardCharsets.UTF_8));
    }


}
