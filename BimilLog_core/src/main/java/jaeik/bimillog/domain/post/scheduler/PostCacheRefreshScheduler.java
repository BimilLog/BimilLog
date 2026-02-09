package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.service.PostCacheRefresh;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <h2>캐시 갱신 스케줄러</h2>
 * <p>1분마다 실시간/주간/레전드 캐시를 갱신합니다.</p>
 * <p>공지사항은 관리자 토글 시 즉시 HSET으로 반영되므로 스케줄러에서 제외됩니다.</p>
 * <p>분산 락을 사용하여 다중 인스턴스 환경에서 하나의 인스턴스만 갱신을 수행합니다.</p>
 * <p>재시도는 {@link PostCacheRefresh}의 {@code @Retryable}이 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "캐시 갱신 스케줄러")
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheRefreshScheduler {

    private final PostCacheRefresh postCacheRefresh;
    private final RealtimePostSync realtimePostSync;
    private final RedisSimplePostAdapter redisSimplePostAdapter;

    /**
     * <h3>1분마다 캐시 갱신 실행</h3>
     * <p>분산 락 획득 → REALTIME → WEEKLY → LEGEND 순차 갱신 → 락 해제</p>
     * <p>NOTICE는 관리자 토글 시 즉시 HSET으로 반영되므로 스케줄러에서 제외</p>
     * <p>각 타입은 독립적으로 실행되며, 하나가 실패해도 나머지는 계속 진행합니다.</p>
     */
    @Scheduled(fixedRate = 60000)
    public void refreshAllCaches() {
        String lockValue = redisSimplePostAdapter.tryAcquireSchedulerLock();
        if (lockValue == null) {
            log.debug("[SCHEDULER] 분산 락 획득 실패 - 다른 인스턴스가 갱신 중");
            return;
        }

        try {
            safeRefresh("REALTIME", realtimePostSync::refreshRealtime);
            safeRefresh("WEEKLY", () -> postCacheRefresh.refreshFeatured(PostCacheFlag.WEEKLY));
            safeRefresh("LEGEND", () -> postCacheRefresh.refreshFeatured(PostCacheFlag.LEGEND));
        } finally {
            redisSimplePostAdapter.releaseSchedulerLock(lockValue);
        }
    }

    /**
     * <h3>타입별 안전 실행</h3>
     * <p>@Retryable/@Recover가 예외를 처리하지만, 방어적으로 catch하여
     * 하나의 타입 실패가 다른 타입에 영향을 주지 않도록 합니다.</p>
     */
    private void safeRefresh(String typeName, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("[SCHEDULER] {} 캐시 갱신 실패: {}", typeName, e.getMessage());
        }
    }
}
