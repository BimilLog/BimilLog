package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.RedisKey.FRIENDSHIP_PREFIX;
import static jaeik.bimillog.infrastructure.redis.RedisKey.PIPELINE_BATCH_SIZE;
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
        List<Object> rawResults = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long memberId : memberIdList) {
                connection.setCommands().sRandMember(
                        createFriendKey(memberId).getBytes(StandardCharsets.UTF_8), sampleSize);
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
        byte[] withdrawMemberIdBytes = String.valueOf(withdrawFriendId).getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < friendIdList.size(); i += PIPELINE_BATCH_SIZE) {
            int end = Math.min(i + PIPELINE_BATCH_SIZE, friendIdList.size());
            List<Long> batch = friendIdList.subList(i, end);

            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Long friendId : batch) {
                    connection.setCommands().sRem(
                            createFriendKey(friendId).getBytes(StandardCharsets.UTF_8), withdrawMemberIdBytes);
                }
                return null;
            });
        }
        stringRedisTemplate.delete(createFriendKey(withdrawFriendId));
    }

    /**
     * <h3>전체 친구 관계 Redis 재구축</h3>
     * <p>기존 friend:* 키를 전체 삭제 후, DB 데이터를 기반으로 파이프라인 배치로 재삽입합니다.</p>
     * <p>양방향 친구 관계를 동시에 처리합니다.</p>
     *
     * @param pairs List of long[] — 각 요소: [memberId, friendId]
     */
    public void rebuildAll(List<long[]> pairs) {
        // 1. SCAN으로 friend:* 키 전체 조회 후 삭제
        List<String> keysToDelete = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(FRIENDSHIP_PREFIX + "*")
                        .count(100)
                        .build())) {
            cursor.forEachRemaining(keysToDelete::add);
        }
        if (!keysToDelete.isEmpty()) {
            for (int i = 0; i < keysToDelete.size(); i += PIPELINE_BATCH_SIZE) {
                List<String> batch = keysToDelete.subList(i, Math.min(i + PIPELINE_BATCH_SIZE, keysToDelete.size()));
                stringRedisTemplate.delete(batch);
            }
        }

        // 2. 파이프라인 배치로 SADD 재삽입 (양방향)
        for (int i = 0; i < pairs.size(); i += PIPELINE_BATCH_SIZE) {
            int end = Math.min(i + PIPELINE_BATCH_SIZE, pairs.size());
            List<long[]> batch = pairs.subList(i, end);

            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (long[] pair : batch) {
                    byte[] memberKey = createFriendKey(pair[0]).getBytes(StandardCharsets.UTF_8);
                    byte[] friendKey = createFriendKey(pair[1]).getBytes(StandardCharsets.UTF_8);
                    byte[] memberIdBytes = String.valueOf(pair[0]).getBytes(StandardCharsets.UTF_8);
                    byte[] friendIdBytes = String.valueOf(pair[1]).getBytes(StandardCharsets.UTF_8);
                    connection.setCommands().sAdd(memberKey, friendIdBytes);
                    connection.setCommands().sAdd(friendKey, memberIdBytes);
                }
                return null;
            });
        }
    }

    /**
     * DLQ 파이프라인 복구 — 친구 추가
     */
    public void processFriendAdd(RedisConnection connection, FriendEventDlq event) {
        connection.setCommands().sAdd(
                createFriendKey(event.getMemberId()).getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getTargetId()).getBytes(StandardCharsets.UTF_8));
        connection.setCommands().sAdd(
                createFriendKey(event.getTargetId()).getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getMemberId()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * DLQ 파이프라인 복구 — 친구 삭제
     */
    public void processFriendRemove(RedisConnection connection, FriendEventDlq event) {
        connection.setCommands().sRem(
                createFriendKey(event.getMemberId()).getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getTargetId()).getBytes(StandardCharsets.UTF_8));
        connection.setCommands().sRem(
                createFriendKey(event.getTargetId()).getBytes(StandardCharsets.UTF_8),
                String.valueOf(event.getMemberId()).getBytes(StandardCharsets.UTF_8));
    }
}
