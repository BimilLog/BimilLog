package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiFunction;

import static jaeik.bimillog.infrastructure.redis.RedisKey.PIPELINE_BATCH_SIZE;

/**
 * <h2>친구 도메인 Redis 복구 어드민 서비스</h2>
 * <p>Redis 데이터 유실 시 DB 데이터를 기반으로 친구 관계 및 상호작용 점수를 재구축합니다.</p>
 * <p>각 테이블을 PK 기준으로 순차 스캔하며 청크마다 Redis에 즉시 기록합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendAdminService {

    private final FriendAdminQueryRepository friendAdminQueryRepository;
    private final RedisFriendRestore redisFriendRestore;

    /**
     * <h3>친구 관계 Redis 스트리밍 재구축</h3>
     * <p>friendship 테이블을 id 기준 순차 스캔하며 청크마다 SADD합니다.</p>
     *
     * @return 처리 결과 메시지
     */
    public String rebuildFriendshipRedis() {
        redisFriendRestore.deleteAllFriendshipKeys();

        long afterId = 0L;
        long totalPairs = 0L;
        List<long[]> chunk;

        do {
            chunk = friendAdminQueryRepository.getFriendshipPairsChunk(afterId, PIPELINE_BATCH_SIZE);
            if (!chunk.isEmpty()) {
                // chunk 원소: [id, memberId, friendId] — Redis에는 [memberId, friendId]만 전달
                List<long[]> pairs = chunk.stream()
                        .map(arr -> new long[]{arr[1], arr[2]})
                        .toList();
                redisFriendRestore.rebuildBatch(pairs);
                afterId = chunk.getLast()[0];
                totalPairs += chunk.size();
            }
        } while (chunk.size() == PIPELINE_BATCH_SIZE);

        return String.format("친구 관계 Redis 재구축 완료. 처리된 친구 쌍: %d개", totalPairs);
    }

    /**
     * <h3>상호작용 점수 Redis 스트리밍 재구축</h3>
     * <p>post_like, comment, comment_like 테이블을 각각 PK 기준 순차 스캔하며 행마다 ZINCRBY합니다.</p>
     * <p>점수 상한(10점)은 재구축 중 적용하지 않으며 조회 시점에 자연스럽게 간주합니다.</p>
     *
     * @return 처리 결과 메시지
     */
    public String rebuildInteractionScoreRedis() {
        redisFriendRestore.deleteAllInteractionKeys();

        long totalRows = 0L;
        totalRows += streamInteractionToRedis(
                (driveId, joinId) -> friendAdminQueryRepository.getPostLikeInteractionsChunk(driveId, joinId, PIPELINE_BATCH_SIZE));
        totalRows += streamInteractionToRedis(
                (driveId, joinId) -> friendAdminQueryRepository.getCommentInteractionsChunk(driveId, joinId, PIPELINE_BATCH_SIZE));
        totalRows += streamInteractionToRedis(
                (driveId, joinId) -> friendAdminQueryRepository.getCommentLikeInteractionsChunk(driveId, joinId, PIPELINE_BATCH_SIZE));

        return String.format("상호작용 점수 Redis 재구축 완료. 처리된 행: %d개", totalRows);
    }

    /**
     * <h3>상호작용 소스 스트리밍 공통 처리</h3>
     * <p>복합 keyset (driveId, joinId)으로 순차 스캔하며 각 청크를 Redis에 ZINCRBY합니다.</p>
     * <p>반환된 chunk 원소: [driveId, joinId, memberId, targetId]</p>
     *
     * @param fetcher (afterDriveId, afterJoinId) → 청크를 반환하는 함수
     * @return 처리된 총 행 수
     */
    private long streamInteractionToRedis(BiFunction<Long, Long, List<long[]>> fetcher) {
        long afterDriveId = 0L;
        long afterJoinId = 0L;
        long count = 0L;
        List<long[]> chunk;

        do {
            chunk = fetcher.apply(afterDriveId, afterJoinId);
            if (!chunk.isEmpty()) {
                redisFriendRestore.incrementInteractionBatch(chunk);
                long[] last = chunk.getLast();
                afterDriveId = last[0];
                afterJoinId = last[1];
                count += chunk.size();
            }
        } while (chunk.size() == PIPELINE_BATCH_SIZE);

        return count;
    }
}
