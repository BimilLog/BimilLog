package jaeik.bimillog.infrastructure.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class AsyncExecutorStatusLogger {
    private final ThreadPoolTaskExecutor cacheRefreshExecutor;

    public AsyncExecutorStatusLogger(@Qualifier("cacheRefreshExecutor") ThreadPoolTaskExecutor cacheRefreshExecutor) {
        this.cacheRefreshExecutor = cacheRefreshExecutor;
    }

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void monitorExecutor() {
        int activeCount = cacheRefreshExecutor.getActiveCount();
        int poolSize = cacheRefreshExecutor.getPoolSize();
        int queueSize = cacheRefreshExecutor.getThreadPoolExecutor().getQueue().size();
        log.info("[Monitor] 활성: {}, 풀크기: {}, 대기열: {}", activeCount, poolSize, queueSize);
    }
}
