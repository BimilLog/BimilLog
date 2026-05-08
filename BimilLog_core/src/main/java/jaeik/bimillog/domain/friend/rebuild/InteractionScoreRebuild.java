package jaeik.bimillog.domain.friend.rebuild;

import jaeik.bimillog.domain.friend.dto.InteractionRebuildDTO;
import jaeik.bimillog.domain.friend.event.FriendEvent.RebuildCompletedEvent;
import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <h2>친구 상호작용 점수 Redis 재구축 서비스</h2>
 * <p>DB 집계 데이터를 기반으로 Redis interaction:* ZSet을 재구축합니다.</p>
 * <p>프로듀서 5개가 동일한 memberQueue에서 경쟁적으로 drainTo하여 병렬 처리합니다.</p>
 * <p>모든 프로듀서가 완료되면 POISON_PILL을 삽입하여 컨슈머에 종료 신호를 전달합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Service
@Slf4j
public class InteractionScoreRebuild extends RebuildTemplate {
    private final FriendRebuildProducer friendRebuildProducer;
    private final FriendRebuildConsumer friendRebuildConsumer;
    private final ApplicationEventPublisher eventPublisher;

    private static final InteractionRebuildDTO POISON_PILL = InteractionRebuildDTO.createDTO(-1L, Map.of());
    private static final int QUEUE_CAPACITY = 10_000;
    private static final int INTERACTION_PRODUCER_COUNT = 5;



    public InteractionScoreRebuild(RedisFriendRestore redisFriendRestore, FriendAdminQueryRepository friendAdminQueryRepository,
                                   FriendRebuildProducer friendRebuildProducer, FriendRebuildConsumer friendRebuildConsumer,
                                   ApplicationEventPublisher eventPublisher) {
        super(redisFriendRestore, friendAdminQueryRepository);
        this.friendRebuildProducer = friendRebuildProducer;
        this.friendRebuildConsumer = friendRebuildConsumer;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void cleanupRedis(RedisFriendRestore redisFriendRestore) {
        redisFriendRestore.deleteAllInteractionKeys();
    }

    @Override
    protected void executeRebuild(LinkedBlockingQueue<Long> memberQueue) {
        BlockingQueue<InteractionRebuildDTO> interactionQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        CompletableFuture<?>[] producers = new CompletableFuture[INTERACTION_PRODUCER_COUNT];
        for (int i = 0; i < INTERACTION_PRODUCER_COUNT; i++) {
            producers[i] = friendRebuildProducer.produceInteraction(memberQueue, interactionQueue);
        }

        CompletableFuture.allOf(producers).thenRun(() -> {
            try {
                interactionQueue.put(POISON_PILL);
                log.info("상호작용 프로듀서 {}개 완료, POISON_PILL 삽입", INTERACTION_PRODUCER_COUNT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("POISON_PILL 삽입 중 인터럽트", e);
            }
        });

        friendRebuildConsumer.consumeInteraction(interactionQueue, POISON_PILL)
                .whenComplete((result, ex) -> {
                    stopRebuilding();
                    if (ex != null) {
                        log.error("[상호작용 점수 재구축] 실패, 플래그 해제", ex);
                        return;
                    }
                    log.info("[상호작용 점수 재구축] 완료, DLQ 재처리 이벤트 발행");
                    eventPublisher.publishEvent(new RebuildCompletedEvent());
                });
    }
}
