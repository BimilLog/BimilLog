package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.async.FriendRebuildConsumer;
import jaeik.bimillog.domain.friend.async.FriendRebuildProducer;
import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;

import static jaeik.bimillog.infrastructure.redis.RedisKey.PIPELINE_BATCH_SIZE;

/**
 * <h2>친구 도메인 Redis 복구 어드민 서비스</h2>
 * <p>Redis 데이터 유실 시 DB 데이터를 기반으로 친구 관계 및 상호작용 점수를 재구축합니다.</p>
 * <p>친구 관계 재구축은 프로듀서/컨슈머 CompletableFuture 병렬 구조로 동작합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendAdminService {

    private static final int QUEUE_CAPACITY = 10_000;

    private static final FriendshipRebuildDTO POISON_PILL =
            FriendshipRebuildDTO.createDTO(-1L, Set.of());

    private final FriendAdminQueryRepository friendAdminQueryRepository;
    private final RedisFriendRestore redisFriendRestore;
    private final FriendRebuildProducer friendRebuildProducer;
    private final FriendRebuildConsumer friendRebuildConsumer;

    /**
     * <h3>친구 관계 Redis 프로듀서/컨슈머 병렬 재구축</h3>
     * <p>전체 memberId를 한 번에 조회하여 LinkedBlockingQueue에 적재한 뒤,
     * 프로듀서가 drainTo로 청크 단위로 꺼내며 배치 친구 조회 → 결과 큐에 삽입합니다.</p>
     * <p>컨슈머: 큐에서 DTO를 꺼내 Redis SADD 수행</p>
     * <p>POISON_PILL 패턴으로 종료 신호를 전달합니다.</p>
     */
    public void getFriendshipDB() {
        redisFriendRestore.deleteAllFriendshipKeys();
        BlockingQueue<FriendshipRebuildDTO> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        List<Long> allIds = friendAdminQueryRepository.getMemberId();
        LinkedBlockingQueue<Long> memberQueue = new LinkedBlockingQueue<>(allIds);

        friendRebuildProducer.produce(memberQueue, queue, POISON_PILL);
        friendRebuildConsumer.consume(queue, POISON_PILL);
    }

    /**
     * <h3>상호작용 점수 Redis 스트리밍 재구축</h3>
     * <p>post_like, comment, comment_like 테이블을 각각 PK 기준 순차 스캔하며 행마다 ZINCRBY합니다.</p>
     * <p>점수 상한(10점)은 재구축 중 적용하지 않으며 조회 시점에 자연스럽게 간주합니다.</p>
     *
     * @return 처리 결과 메시지
     */
    @Transactional(readOnly = true)
    public String rebuildInteractionScoreRedis() {
        redisFriendRestore.deleteAllInteractionKeys();

        long totalRows = 0L;

        log.info("DB 게시글 추천 조회 요청");
        BiFunction<Long, Long, List<long[]>> postLikeFetcher =
                (driveId, joinId) -> friendAdminQueryRepository.getPostLikeInteractionsChunk(driveId, joinId, PIPELINE_BATCH_SIZE);

        log.info("DB 댓글 조회 요청");
        BiFunction<Long, Long, List<long[]>> commentFetcher =
                (driveId, joinId) -> friendAdminQueryRepository.getCommentInteractionsChunk(driveId, joinId, PIPELINE_BATCH_SIZE);

        log.info("DB 댓글 추천 조회 요청");
        BiFunction<Long, Long, List<long[]>> commentLikeFetcher =
                (driveId, joinId) -> friendAdminQueryRepository.getCommentLikeInteractionsChunk(driveId, joinId, PIPELINE_BATCH_SIZE);

        totalRows += streamInteractionToRedis(postLikeFetcher);
        totalRows += streamInteractionToRedis(commentFetcher);
        totalRows += streamInteractionToRedis(commentLikeFetcher);

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

        while (true) {
            List<long[]> chunk = fetcher.apply(afterDriveId, afterJoinId);
            if (chunk.isEmpty()) break;

            log.info("레디스 상호관계점수 삽입 시작");
            redisFriendRestore.incrementInteractionBatch(chunk);
            log.info("레디스 상호관계점수 삽입 1000개 완료");
            long[] last = chunk.getLast();
            afterDriveId = last[0];
            afterJoinId = last[1];
            count += chunk.size();
            if (chunk.size() < PIPELINE_BATCH_SIZE) break;
        }

        return count;
    }
}
