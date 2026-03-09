package jaeik.bimillog.infrastructure.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <h2>친구 관련 비동기 스레드 풀 설정</h2>
 * <p>친구 관계 업데이트, 재구축 프로듀서/컨슈머, 상호작용 점수 스레드 풀을 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Configuration
public class FriendAsyncConfig {

    /**
     * 친구 관계 추가 친구 상호작용 점수 전용 스레드 풀
     */
    @Bean(name = "friendUpdateExecutor")
    public Executor friendUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("friend-update-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 친구 관계 재구축 프로듀서 스레드 풀
     * <p>DB 배치 조회를 병렬 수행합니다. (IO-bound)</p>
     */
    @Bean(name = "rebuildProducerExecutor")
    public Executor rebuildProducerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("rebuild-producer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * 친구 관계 재구축 컨슈머 스레드 풀
     * <p>Redis SADD 파이프라인 쓰기를 수행합니다.</p>
     */
    @Bean(name = "rebuildConsumerExecutor")
    public Executor rebuildConsumerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("rebuild-consumer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * 상호작용 점수 재구축 프로듀서 스레드 풀
     * <p>DB GROUP BY 조회를 병렬 수행합니다. (IO-bound, 프로듀서 5개 동시 실행)</p>
     */
    @Bean(name = "interactionProducerExecutor")
    public Executor interactionProducerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("interaction-producer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * 상호작용 점수 재구축 컨슈머 스레드 풀
     * <p>Redis ZADD 파이프라인 쓰기를 수행합니다.</p>
     */
    @Bean(name = "interactionConsumerExecutor")
    public Executor interactionConsumerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("interaction-consumer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
