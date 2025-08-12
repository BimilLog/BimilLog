package jaeik.growfarm.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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
        executor.initialize();
        return executor;
    }
}