package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.service.PostInteractionService;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <h2>조회수 플러시 스케줄러</h2>
 * <p>Redis Hash에 버퍼링된 조회수를 5분마다 DB에 일괄 반영합니다.</p>
 * <p>분산 락을 사용하여 다중 인스턴스 환경에서 중복 실행을 방지합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewCountFlushScheduler {
    private final RedisPostViewAdapter redisPostViewAdapter;
    private final PostInteractionService postInteractionService;

    /**
     * <h3>조회수 일괄 반영</h3>
     * <p>5분마다 Redis에서 누적된 조회수를 가져와 DB에 벌크 UPDATE합니다.</p>
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void flushViewCounts() {
        if (!redisPostViewAdapter.tryAcquireFlushLock()) {
            return;
        }

        try {
            Map<Long, Long> counts = redisPostViewAdapter.getAndClearViewCounts();

            if (counts.isEmpty()) {
                return;
            }

            postInteractionService.bulkIncrementViewCounts(counts);
            log.info("조회수 플러시 완료: {}개 게시글 반영", counts.size());
        } catch (Exception e) {
            log.error("조회수 플러시 실패", e);
        }
    }
}
