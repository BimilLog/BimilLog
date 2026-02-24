package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.RedisKey.createFriendKey;

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
    private static final int PIPELINE_BATCH_SIZE = 500;

    /**
     * <h3>특정 회원의 1촌 친구 목록 랜덤 조회</h3>
     */
    public Set<Long> getFriendIdRandom(Long memberId, int count) {
        Set<String> members = stringRedisTemplate.opsForSet().distinctRandomMembers(createFriendKey(memberId), count);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    /**
     * <h3>특정 회원의 1촌 친구 목록 전체 조회</h3>
     */
    public Set<Long> getFriendId(Long memberId) {
        Set<String> members = stringRedisTemplate.opsForSet().members(createFriendKey(memberId));
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
    public List<List<Long>> getFriendsBatch(List<Long> memberIdList, int sampleSize) {
        List<Object> rawResults =  stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long memberId : memberIdList) {
                stringRedisTemplate.opsForSet().distinctRandomMembers(createFriendKey(memberId), sampleSize);
            }
            return null;
        });

        return rawResults.stream()
                .map(this::convertToLongList)
                .toList();
    }

    private List<Long> convertToLongList(Object result) {
        if (!(result instanceof Collection<?>)) {
            return Collections.emptyList();
        }

        return ((Collection<?>) result).stream()
                .map(item -> Long.parseLong(item.toString()))
                .toList();
    }

    /**
     * <h3>레디스 친구 증가</h3>
     */
    public void addFriend(Long memberId, Long friendId) {
        stringRedisTemplate.opsForSet().add(createFriendKey(memberId), String.valueOf(friendId));
        stringRedisTemplate.opsForSet().add(createFriendKey(friendId), String.valueOf(memberId));
    }

    /**
     * <h3>레디스 친구 삭제</h3>
     */
    public void deleteFriend(Long memberId, Long friendId) {
        stringRedisTemplate.opsForSet().remove(createFriendKey(memberId), String.valueOf(friendId));
        stringRedisTemplate.opsForSet().remove(createFriendKey(friendId), String.valueOf(memberId));
    }

    /**
     * <h3>회원 탈퇴상항 친구 삭제</h3>
     * <p>레디스 친구관계 테이블에서 탈퇴한 회원을 삭제한다.</p>
     * <p>탈퇴 회원의 친구 목록을 기반으로 타겟 키만 정리한다.</p>
     */
    public void deleteWithdrawFriendTargeted(List<Long> friendIdList, Long withdrawFriendId) {
        for (int i = 0; i < friendIdList.size(); i += PIPELINE_BATCH_SIZE) {
            int end = Math.min(i + PIPELINE_BATCH_SIZE, friendIdList.size());
            List<Long> batch = friendIdList.subList(i, end);

            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Long friendId : batch) {
                    stringRedisTemplate.opsForSet().remove(createFriendKey(friendId), String.valueOf(withdrawFriendId));
                }
                return null;
            });
        }
        stringRedisTemplate.delete(createFriendKey(withdrawFriendId));
    }
}
