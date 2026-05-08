package jaeik.bimillog.domain.friend.rebuild;

import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.domain.friend.event.FriendEvent.RebuildCompletedEvent;
import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <h2>친구 관계 Redis 재구축 서비스</h2>
 * <p>DB의 friendship 데이터를 기반으로 Redis friend:* Set을 재구축합니다.</p>
 * <p>프로듀서 1개 → 컨슈머 1개 구조로 동작합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Service
@Slf4j
public class FriendshipRebuild extends RebuildTemplate {
    private final FriendRebuildProducer friendRebuildProducer;
    private final FriendRebuildConsumer friendRebuildConsumer;
    private final ApplicationEventPublisher eventPublisher;

    private static final FriendshipRebuildDTO POISON_PILL = FriendshipRebuildDTO.createDTO(-1L, Set.of());
    private static final int QUEUE_CAPACITY = 10_000;

    public FriendshipRebuild(RedisFriendRestore redisFriendRestore, FriendAdminQueryRepository friendAdminQueryRepository,
                             FriendRebuildProducer friendRebuildProducer, FriendRebuildConsumer friendRebuildConsumer,
                             ApplicationEventPublisher eventPublisher) {
        super(redisFriendRestore, friendAdminQueryRepository);
        this.friendRebuildProducer = friendRebuildProducer;
        this.friendRebuildConsumer = friendRebuildConsumer;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void cleanupRedis(RedisFriendRestore redisFriendRestore) {
        redisFriendRestore.deleteAllFriendshipKeys();
    }

    @Override
    protected void executeRebuild(LinkedBlockingQueue<Long> memberQueue) {
        BlockingQueue<FriendshipRebuildDTO> relationQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        friendRebuildProducer.produce(memberQueue, relationQueue, POISON_PILL);
        friendRebuildConsumer.consume(relationQueue, POISON_PILL)
                .whenComplete((result, ex) -> {
                    stopRebuilding();
                    if (ex != null) {
                        log.error("[친구 관계 재구축] 실패, 플래그 해제", ex);
                        return;
                    }
                    log.info("[친구 관계 재구축] 완료, DLQ 재처리 이벤트 발행");
                    eventPublisher.publishEvent(new RebuildCompletedEvent());
                });
    }
}
