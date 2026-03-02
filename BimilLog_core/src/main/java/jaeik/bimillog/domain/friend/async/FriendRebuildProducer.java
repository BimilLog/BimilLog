package jaeik.bimillog.domain.friend.async;

import jaeik.bimillog.domain.friend.dto.FriendshipRebuildDTO;
import jaeik.bimillog.domain.friend.dto.InteractionRebuildDTO;
import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

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
    private static final int INTERACTION_CHUNK_SIZE = 500;

    private final FriendAdminQueryRepository friendAdminQueryRepository;

    /**
     * <h3>DB 배치 조회 → 큐 삽입</h3>
     * <p>완료 시 POISON_PILL을 삽입하여 컨슈머에 종료 신호를 전달합니다.</p>
     */
    @Async("rebuildProducerExecutor")
    public void produce(LinkedBlockingQueue<Long> memberQueue, BlockingQueue<FriendshipRebuildDTO> queue,
                        FriendshipRebuildDTO poisonPill) {
        List<Long> memberIds = new ArrayList<>(MEMBER_CHUNK_SIZE);

        try {
            while (!memberQueue.isEmpty()) {
                memberIds.clear();
                memberQueue.drainTo(memberIds, MEMBER_CHUNK_SIZE);
                if (memberIds.isEmpty()) break;

                Map<Long, Set<Long>> friendMap = friendAdminQueryRepository.getMemberFriendBatch(memberIds);
                log.info("친구 관계 생산자 : memberId {} 이후 {}명 처리 완료", memberIds.getLast(), memberIds.size());

                for (Map.Entry<Long, Set<Long>> entry : friendMap.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        FriendshipRebuildDTO dto = FriendshipRebuildDTO.createDTO(
                                entry.getKey(), entry.getValue());
                        queue.put(dto);
                        log.debug("DB 친구관계 큐 삽입 : 아이디 {}", entry.getKey());
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("친구관계 프로듀서 인터럽트 발생", e);
        } finally {
            try {
                queue.put(poisonPill);
                log.info("친구관계 프로듀서: POISON_PILL 삽입, 종료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("친구관계 프로듀서 : POISON_PILL 삽입 중 인터럽트", e);
            }
        }
    }

    /**
     * <h3>상호작용 점수 DB 배치 조회 → 큐 삽입</h3>
     * <p>여러 프로듀서가 동일한 memberQueue에서 경쟁적으로 drainTo하여 병렬 처리합니다.</p>
     * <p>POISON_PILL은 호출측에서 allOf 완료 후 삽입합니다.</p>
     */
    @Async("interactionProducerExecutor")
    public CompletableFuture<Void> produceInteraction(LinkedBlockingQueue<Long> memberQueue,
                                                      BlockingQueue<InteractionRebuildDTO> queue) {
        List<Long> memberIds = new ArrayList<>(INTERACTION_CHUNK_SIZE);

        try {
            while (!memberQueue.isEmpty()) {
                memberIds.clear();
                memberQueue.drainTo(memberIds, INTERACTION_CHUNK_SIZE);
                if (memberIds.isEmpty()) break;

                Map<Long, Map<Long, Double>> scoreMap = friendAdminQueryRepository.getInteractionScore(memberIds);
                log.info("상호작용 생산자 : memberId {} 이후 {}명 처리 완료", memberIds.getLast(), memberIds.size());

                for (Map.Entry<Long, Map<Long, Double>> entry : scoreMap.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        InteractionRebuildDTO dto = InteractionRebuildDTO.createDTO(
                                entry.getKey(), entry.getValue());
                        queue.put(dto);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("상호작용 프로듀서 : 인터럽트 발생", e);
        }

        log.info("상호작용 프로듀서 : 종료");
        return CompletableFuture.completedFuture(null);
    }
}
