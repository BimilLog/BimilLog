package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static jaeik.bimillog.infrastructure.redis.RedisKey.*;
import static jaeik.bimillog.infrastructure.redis.RedisKey.PIPELINE_BATCH_SIZE;
import static jaeik.bimillog.infrastructure.redis.RedisKey.createAllInteractionKey;
import static jaeik.bimillog.infrastructure.redis.RedisKey.createInteractionKey;

/**
 * <h2>레디스 복구 관련 저장소</h2>
 * @author Jaeik
 * @version 2.8.0
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisFriendRestore {
    private final StringRedisTemplate stringRedisTemplate;

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


    /**
     * DLQ 파이프라인 복구 — 상호작용 점수 증가
     */
    public void processScoreUp(RedisConnection connection, FriendEventDlq event) {
        double increment = event.getScore() != null ? event.getScore() : INTERACTION_SCORE_DEFAULT;

        connection.zSetCommands().zIncrBy(
                createInteractionKey(event.getMemberId()).getBytes(StandardCharsets.UTF_8),
                increment,
                event.getTargetId().toString().getBytes(StandardCharsets.UTF_8));
        connection.zSetCommands().zIncrBy(
                createInteractionKey(event.getTargetId()).getBytes(StandardCharsets.UTF_8),
                increment,
                event.getMemberId().toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * <h3>상호작용 점수 Redis 키 전체 삭제</h3>
     * <p>SCAN으로 interaction:* 키를 순회하며 배치 삭제합니다.</p>
     * <p>스트리밍 재구축 시 삽입 전 한 번만 호출합니다.</p>
     */
    public void deleteAllInteractionKeys() {
        List<String> keysToDelete = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(createAllInteractionKey())
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
    }

    /**
     * <h3>친구 관계 Redis 키 전체 삭제</h3>
     * <p>SCAN으로 friend:* 키를 순회하며 배치 삭제합니다.</p>
     * <p>스트리밍 재구축 시 삽입 전 한 번만 호출합니다.</p>
     */
    public void deleteAllFriendshipKeys() {
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
    }

    /**
     * <h3>친구 관계 청크 Redis 삽입</h3>
     * <p>전달된 pairs를 파이프라인 한 번으로 양방향 SADD 처리합니다.</p>
     * <p>스트리밍 재구축에서 DB 청크마다 호출됩니다.</p>
     *
     * @param pairs List of long[] — 각 요소: [memberId, friendId]
     */
    public void rebuildBatch(List<long[]> pairs) {
        if (pairs.isEmpty()) {
            return;
        }
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (long[] pair : pairs) {
                byte[] memberKey = createFriendKey(pair[0]).getBytes(StandardCharsets.UTF_8);
                byte[] friendKey = createFriendKey(pair[1]).getBytes(StandardCharsets.UTF_8);
                byte[] memberIdBytes = String.valueOf(pair[0]).getBytes(StandardCharsets.UTF_8);
                byte[] friendIdBytes = String.valueOf(pair[1]).getBytes(StandardCharsets.UTF_8);
                connection.setCommands().sAdd(memberKey, friendIdBytes);
                connection.setCommands().sAdd(friendKey, memberIdBytes);
            }
            return null;
        });
        log.info("레디스 친구 관계 삽입 1000개 완료");
    }

    /**
     * <h3>상호작용 점수 청크 Redis 증분 삽입</h3>
     * <p>ZINCRBY로 원시 행 1건당 INTERACTION_SCORE_DEFAULT(0.5)씩 누적합니다.</p>
     * <p>post_like, comment, comment_like 세 소스에서 순차적으로 호출되며,
     * 동일 쌍의 점수는 Redis에서 자동 합산됩니다.</p>
     * <p>점수 상한은 별도 처리 없이 조회 시점에 자연스럽게 간주합니다.</p>
     *
     * @param batch long[3][] — 각 요소: [rowId(무시), memberId, targetId]
     */
    public void incrementInteractionBatch(List<long[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (long[] row : batch) {
                // row[0] = driveId (keyset), row[1] = joinId (keyset)
                // row[2] = memberId, row[3] = targetId
                connection.zSetCommands().zIncrBy(
                        createInteractionKey(row[2]).getBytes(StandardCharsets.UTF_8),
                        INTERACTION_SCORE_DEFAULT,
                        String.valueOf(row[3]).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        log.info("레디스 친구 관계 삽입 1000개 완료");
    }
}
