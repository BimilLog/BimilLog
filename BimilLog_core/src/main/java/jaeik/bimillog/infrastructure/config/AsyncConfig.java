package jaeik.bimillog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <h2>비동기 처리 설정</h2>
 * <p>
 * SSE 알림, FCM 알림 등 비동기 처리를 위한 스레드 풀 설정
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {

    /**
     * SSE 알림 전용 스레드 풀
     */
    @Bean(name = "sseNotificationExecutor")
    public Executor sseNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3); // 기본 스레드 수
        executor.setMaxPoolSize(10); // 최대 스레드 수
        executor.setQueueCapacity(50); // 대기열 크기
        executor.setThreadNamePrefix("sse-notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 대기열 가득 차면 호출 스레드에서 실행
        executor.initialize();
        return executor;
    }

    /**
     * FCM 알림 전용 스레드 풀
     */
    @Bean(name = "fcmNotificationExecutor")
    public Executor fcmNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 기본 스레드 수 (FCM은 외부 API 호출이므로 적게)
        executor.setMaxPoolSize(8); // 최대 스레드 수
        executor.setQueueCapacity(100); // 대기열 크기 (FCM은 지연되어도 괜찮으므로 크게)
        executor.setThreadNamePrefix("fcm-notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // FCM은 시간이 오래 걸릴 수 있으므로 길게
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 대기열 가득 차면 호출 스레드에서 실행
        executor.initialize();
        return executor;
    }

    /**
     * 알림 저장 전용 스레드 풀
     */
    @Bean(name = "saveNotificationExecutor")
    public Executor saveNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 기본 스레드 수
        executor.setMaxPoolSize(30); // 최대 스레드 수
        executor.setQueueCapacity(70); // 대기열 크기
        executor.setThreadNamePrefix("save-notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 대기열 가득 차면 호출 스레드에서 실행
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 캐시 갱신 전용 스레드 풀
     * <p>PER 기법에서 비동기 캐시 갱신에 사용됩니다.</p>
     */
    @Bean(name = "cacheRefreshExecutor")
    public Executor cacheRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 기본 스레드 수
        executor.setMaxPoolSize(5); // 최대 스레드 수
        executor.setQueueCapacity(50); // 대기열 크기
        executor.setThreadNamePrefix("cache-refresh-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 대기열 가득 차면 호출 스레드에서 실행
        executor.initialize();
        return executor;
    }
}