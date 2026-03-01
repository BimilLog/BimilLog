package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.async.FriendRebuildConsumer;
import jaeik.bimillog.domain.friend.async.FriendRebuildProducer;
import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.domain.friend.dto.InteractionRebuildDTO;
import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    private static final InteractionRebuildDTO INTERACTION_POISON_PILL =
            InteractionRebuildDTO.createDTO(-1L, Map.of());

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
     * <h3>상호작용 점수 Redis 프로듀서/컨슈머 병렬 재구축</h3>
     * <p>전체 memberId를 한 번에 조회하여 LinkedBlockingQueue에 적재한 뒤,
     * 프로듀서가 drainTo로 청크 단위로 꺼내며 배치 상호작용 조회 → 결과 큐에 삽입합니다.</p>
     * <p>컨슈머: 큐에서 DTO를 꺼내 Redis ZADD 수행</p>
     * <p>POISON_PILL 패턴으로 종료 신호를 전달합니다.</p>
     */
    public void rebuildInteractionScoreRedis() {
        redisFriendRestore.deleteAllInteractionKeys();
        BlockingQueue<InteractionRebuildDTO> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        List<Long> allIds = friendAdminQueryRepository.getMemberId();
        LinkedBlockingQueue<Long> memberQueue = new LinkedBlockingQueue<>(allIds);

        friendRebuildProducer.produceInteraction(memberQueue, queue, INTERACTION_POISON_PILL);
        friendRebuildConsumer.consumeInteraction(queue, INTERACTION_POISON_PILL);
    }
}
