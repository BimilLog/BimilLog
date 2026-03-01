package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.domain.friend.dto.InteractionRebuildDTO;
import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import org.springframework.data.redis.connection.DefaultTuple;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
     * <h3>친구 관계 파이프라인 배치 Redis 삽입</h3>
     * <p>여러 멤버의 친구 관계를 파이프라인 한 번으로 SADD 처리합니다.</p>
     * <p>각 멤버별로 가변인자 SADD를 파이프라인 안에서 실행합니다.</p>
     */
    public void rebuildPipelineBatch(List<FriendshipRebuildDTO> batch) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (FriendshipRebuildDTO dto : batch) {
                byte[] key = createFriendKey(dto.getMemberId()).getBytes(StandardCharsets.UTF_8);
                byte[][] members = dto.getFriendIds().stream()
                        .map(id -> String.valueOf(id).getBytes(StandardCharsets.UTF_8))
                        .toArray(byte[][]::new);
                connection.setCommands().sAdd(key, members);
            }
            return null;
        });
    }

    /**
     * <h3>상호작용 점수 파이프라인 배치 Redis 삽입</h3>
     * <p>여러 멤버의 상호작용 점수를 파이프라인 한 번으로 ZADD 처리합니다.</p>
     * <p>각 멤버별로 (targetId, score) 쌍을 ZADD합니다.</p>
     */
    public void rebuildInteractionPipelineBatch(List<InteractionRebuildDTO> batch) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (InteractionRebuildDTO dto : batch) {
                byte[] key = createInteractionKey(dto.getMemberId()).getBytes(StandardCharsets.UTF_8);
                Set<DefaultTuple> tuples = dto.getScores().entrySet().stream()
                        .map(e -> new DefaultTuple(
                                String.valueOf(e.getKey()).getBytes(StandardCharsets.UTF_8),
                                e.getValue()))
                        .collect(Collectors.toSet());
                connection.zSetCommands().zAdd(key, tuples);
            }
            return null;
        });
    }
}
