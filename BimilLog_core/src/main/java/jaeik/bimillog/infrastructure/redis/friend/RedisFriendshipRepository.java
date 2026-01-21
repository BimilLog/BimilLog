package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
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
    private final RedisTemplate<String, Long> redisTemplate;
    private static final int PIPELINE_BATCH_SIZE = 1000;
    private static final int SECOND_DEGREE_SAMPLE_SIZE = 100;
    private static final int THIRD_DEGREE_SAMPLE_SIZE = 50;

    /**
     * 특정 회원의 1촌 친구 목록 조회
     * 랜덤 200명 추출
     */
    public Set<Long> getFriends(Long memberId, int count) {
        String key = FRIEND_SHIP_PREFIX + memberId;
        return redisTemplate.opsForSet().distinctRandomMembers(key, count);
    }

    /**
     * 여러 회원의 친구 목록을 파이프라인으로 일괄 조회 (랜덤 100명씩)
     *
     * @param memberIds 조회할 회원 ID 목록
     * @return Map<회원ID, 친구ID_Set>
     */
    public Map<Long, Set<Long>> getFriendsBatch(Set<Long> memberIds) {
        List<Long> memberIdList = new ArrayList<>(memberIds);
        Map<Long, Set<Long>> resultMap = new HashMap<>();

        try {
            for (int batchStart = 0; batchStart < memberIdList.size(); batchStart += PIPELINE_BATCH_SIZE) {
                int batchEnd = Math.min(batchStart + PIPELINE_BATCH_SIZE, memberIdList.size());
                List<Long> batch = memberIdList.subList(batchStart, batchEnd);

                // Pipeline 실행 (랜덤 100명씩 조회)
                List<Object> results = redisTemplate.executePipelined((RedisConnection connection) -> {
                    for (Long memberId : batch) {
                        String key = FRIEND_SHIP_PREFIX + memberId;
                        connection.setCommands().sRandMember(key.getBytes(StandardCharsets.UTF_8), SECOND_DEGREE_SAMPLE_SIZE);
                    }
                    return null;
                });

                // 결과 매핑
                for (int i = 0; i < batch.size(); i++) {
                    Long memberId = batch.get(i);
                    Object result = results.get(i);

                    Set<Long> friendIds = new HashSet<>();

                    if (result instanceof List<?> listResult) {
                        for (Object member : listResult) {
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
            }
            return resultMap;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_SHIP_QUERY_ERROR, e);
        }
    }

    /**
     * 3촌 탐색용: 여러 회원의 친구 목록을 파이프라인으로 일괄 조회 (랜덤 50명씩, 배치 없이 한 번에)
     *
     * @param memberIds 조회할 회원 ID 목록
     * @return Map<회원ID, 친구ID_Set>
     */
    public Map<Long, Set<Long>> getFriendsBatchForThirdDegree(Set<Long> memberIds) {
        List<Long> memberIdList = new ArrayList<>(memberIds);
        Map<Long, Set<Long>> resultMap = new HashMap<>();

        try {
            List<Object> results = redisTemplate.executePipelined((RedisConnection connection) -> {
                for (Long memberId : memberIdList) {
                    String key = FRIEND_SHIP_PREFIX + memberId;
                    connection.setCommands().sRandMember(key.getBytes(StandardCharsets.UTF_8), THIRD_DEGREE_SAMPLE_SIZE);
                }
                return null;
            });

            for (int i = 0; i < memberIdList.size(); i++) {
                Long memberId = memberIdList.get(i);
                Object result = results.get(i);

                Set<Long> friendIds = new HashSet<>();

                if (result instanceof List<?> listResult) {
                    for (Object member : listResult) {
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
// 탈퇴 회원의 친구 목록을 기반으로 타겟 키만 정리한다.
    public void deleteWithdrawFriendTargeted(Long withdrawFriendId) {
        try {
            Set<Long> friendIds = getFriends(withdrawFriendId, PIPELINE_BATCH_SIZE);
            if (friendIds != null && !friendIds.isEmpty()) {
                byte[] withdrawMemberIdBytes = String.valueOf(withdrawFriendId).getBytes(StandardCharsets.UTF_8);
                List<Long> friendIdList = new ArrayList<>(friendIds);
                int batchSize = 500;
                for (int i = 0; i < friendIdList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, friendIdList.size());
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
