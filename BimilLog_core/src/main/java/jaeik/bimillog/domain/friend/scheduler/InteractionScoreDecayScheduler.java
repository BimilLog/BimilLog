package jaeik.bimillog.domain.friend.scheduler;

import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <h2>상호작용 점수 지수 감쇠 스케줄러</h2>
 * <p>1일마다 모든 상호작용 점수에 0.95를 곱하여 시간에 따른 감쇠를 적용합니다.</p>
 * <p>오래된 상호작용의 영향력을 자연스럽게 감소시켜 최근 상호작용이 추천에 더 많이 반영되도록 합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionScoreDecayScheduler {

    private final RedisInteractionScoreRepository redisInteractionScoreRepository;

    /**
     * 1일마다 상호작용 점수에 지수 감쇠 적용
     * <p>모든 interaction:* 키의 점수에 0.95를 곱하고, 0.1 이하의 점수는 삭제합니다.</p>
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void applyInteractionScoreDecay() {
        try {
            redisInteractionScoreRepository.applyInteractionScoreDecay();
        } catch (Exception e) {
            log.error("[상호작용 점수 감쇠] 실패", e);
        }
    }
}
