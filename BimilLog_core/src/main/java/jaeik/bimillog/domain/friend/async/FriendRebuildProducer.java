package jaeik.bimillog.domain.friend.async;

import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * <h2>친구 관계 재구축 프로듀서</h2>
 * <p>DB에서 memberId를 청크 단위로 조회하고 배치 친구 조회 결과를 큐에 삽입합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendRebuildProducer {

    private static final int MEMBER_CHUNK_SIZE = 1000;

    private final FriendAdminQueryRepository friendAdminQueryRepository;

    /**
     * <h3>DB 배치 조회 → 큐 삽입</h3>
     * <p>완료 시 POISON_PILL을 삽입하여 컨슈머에 종료 신호를 전달합니다.</p>
     */
    @Async("rebuildProducerExecutor")
    public void produce(BlockingQueue<FriendshipRebuildDTO> queue, FriendshipRebuildDTO poisonPill) {
        try {
            long afterId = 0L;

            while (true) {
                List<Long> chunk = friendAdminQueryRepository.getMemberIdChunk(afterId, MEMBER_CHUNK_SIZE);
                if (chunk.isEmpty()) break;

                Map<Long, Set<Long>> friendMap = friendAdminQueryRepository.getMemberFriendBatch(chunk);
                for (Map.Entry<Long, Set<Long>> entry : friendMap.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        FriendshipRebuildDTO dto = FriendshipRebuildDTO.createDTO(
                                entry.getKey(), entry.getValue());
                        queue.put(dto);
                        log.debug("DB 친구관계 큐 삽입 : 아이디 {}", entry.getKey());
                    }
                }

                afterId = chunk.getLast();
                log.info("프로듀서: memberId {} 이후 {}명 처리 완료", afterId, chunk.size());
                if (chunk.size() < MEMBER_CHUNK_SIZE) break;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("프로듀서 인터럽트 발생", e);
        } finally {
            try {
                queue.put(poisonPill);
                log.info("프로듀서: POISON_PILL 삽입, 종료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("POISON_PILL 삽입 중 인터럽트", e);
            }
        }
    }
}
