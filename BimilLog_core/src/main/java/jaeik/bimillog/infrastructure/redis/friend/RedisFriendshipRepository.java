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
 * @version 2.1.0
 */
@Repository
@RequiredArgsConstructor
public class RedisFriendshipRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 회원의 친구 목록 조회
     */
    public Set<Long> getFriends(Long memberId) {
        String key = FRIEND_SHIP_PREFIX + memberId;
        try {
            Set<Object> friends = redisTemplate.opsForSet().members(key);
            if (friends == null || friends.isEmpty()) {
                return new HashSet<>();
            }
            Set<Long> friendIds = new HashSet<>();
            for (Object friend : friends) {
                friendIds.add(Long.valueOf(friend.toString()));
            }
            return friendIds;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_DELETE_ERROR, e);
        }
    }

    /**
     * 여러 회원의 친구 목록을 파이프라인으로 일괄 조회
     *
     * @param memberIds 조회할 회원 ID 목록
     * @return Map<회원ID, 친구ID_Set>
     */
    public Map<Long, Set<Long>> getFriendsBatch(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Long> memberIdList = new ArrayList<>(memberIds);
        Map<Long, Set<Long>> resultMap = new HashMap<>();

        try {
            // Pipeline 실행
            List<Object> results = redisTemplate.executePipelined((RedisConnection connection) -> {
                for (Long memberId : memberIdList) {
                    String key = FRIEND_SHIP_PREFIX + memberId;
                    connection.setCommands().sMembers(key.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });

            // 결과 매핑
            for (int i = 0; i < memberIdList.size(); i++) {
                Long memberId = memberIdList.get(i);
                Object result = results.get(i);

                Set<Long> friendIds = new HashSet<>();

                if (result instanceof Set<?> setResult) {
                    // 내부 멤버를 Object로 안전하게 처리
                    for (Object member : setResult) {
                        try {
                            friendIds.add(Long.valueOf(member.toString()));
                        } catch (NumberFormatException e) {
                            System.err.println("[WARN] Redis Set 멤버 Long 변환 실패. MemberId: " + memberId + ", Value: " + member);
                        }
                    }
                } else if (result != null) {
                    System.err.println("[ERROR] Redis 조회 결과 예상 타입 아님: " + result.getClass().getName() + ", MemberId: " + memberId);
                }
                resultMap.put(memberId, friendIds);
            }
            return resultMap;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_QUERY_ERROR, e);
        }
    }


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
    // SCAN 패턴 매칭으로 탈퇴한 회원 데이터 삭제
    public void deleteWithdrawFriendByScan(Long withdrawFriendId) {
        try {
            // 1. 탈퇴 회원의 친구 관계 테이블 삭제
            String withdrawKey = FRIEND_SHIP_PREFIX + withdrawFriendId;
            redisTemplate.delete(withdrawKey);

            // 2. SCAN으로 모든 friend 키 조회 후, 탈퇴 회원 데이터 제거
            String pattern = FRIEND_SHIP_PREFIX + "*";
            byte[] withdrawMemberIdBytes = String.valueOf(withdrawFriendId).getBytes(StandardCharsets.UTF_8);

            redisTemplate.execute((RedisConnection connection) -> {
                // SCAN으로 패턴 매칭 키 조회
                connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build())
                        .forEachRemaining(key -> {
                            // 각 키에서 탈퇴 회원 제거
                            connection.setCommands().sRem(key, withdrawMemberIdBytes);
                        });
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_DELETE_ERROR, e);
        }
    }
}
