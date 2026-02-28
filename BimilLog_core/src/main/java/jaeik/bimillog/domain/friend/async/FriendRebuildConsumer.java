package jaeik.bimillog.domain.friend.async;

import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static jaeik.bimillog.infrastructure.redis.RedisKey.PIPELINE_BATCH_SIZE;

/**
 * <h2>친구 관계 재구축 컨슈머</h2>
 * <p>큐에서 DTO를 1000개씩 모아 Redis 파이프라인으로 SADD합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendRebuildConsumer {

    private final RedisFriendRestore redisFriendRestore;

    /**
     * <h3>큐에서 DTO 소비 → 1000개씩 파이프라인 Redis SADD</h3>
     * <p>drainTo(batch, maxElements)로 배치 사이즈만큼만 꺼내 파이프라인 전송합니다.</p>
     * <p>POISON_PILL 수신 시 남은 항목을 처리하고 종료합니다.</p>
     */
    @Async("rebuildConsumerExecutor")
    public void consume(BlockingQueue<FriendshipRebuildDTO> queue,
                        FriendshipRebuildDTO poisonPill) {
        long count = 0L;
        List<FriendshipRebuildDTO> batch = new ArrayList<>(PIPELINE_BATCH_SIZE);

        try {
            while (true) {
                FriendshipRebuildDTO first = queue.poll(5, TimeUnit.SECONDS);
                if (first == null) continue;

                batch.add(first);
                queue.drainTo(batch, PIPELINE_BATCH_SIZE - 1);

                boolean poisonReceived = batch.remove(poisonPill);

                if (!batch.isEmpty()) {
                    redisFriendRestore.rebuildPipelineBatch(batch);
                    count += batch.size();
                    log.info("컨슈머: {}명 Redis 파이프라인 삽입 완료", count);
                }

                batch.clear();

                if (poisonReceived) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("컨슈머 인터럽트 발생", e);
        }

        log.info("컨슈머: 종료. 총 {}명 처리", count);
    }
}
