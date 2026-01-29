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
     * 멤버 로그아웃, 회원탈퇴, 밴 전용 스레드 풀
     * <p>회원 상태 변경 시 발생하는 정리 작업을 처리합니다.</p>
     * <p>SSE 연결 해제, 토큰 삭제, 소셜 계정 연동 해제 등의 작업을 포함합니다.</p>
     */
    @Bean(name = "memberEventExecutor")
    public Executor memberEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 기본 스레드 수 (회원 이벤트는 빈도 낮음)
        executor.setMaxPoolSize(5); // 최대 스레드 수
        executor.setQueueCapacity(30); // 대기열 크기
        executor.setThreadNamePrefix("member-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // 회원 정리 작업은 완료 대기
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 실시간 인기글, 실시간 롤링페이퍼, 친구 상호작용 전용 스레드 풀
     * <p>Redis 기반의 실시간 점수 업데이트를 처리합니다.</p>
     * <p>빠른 응답이 필요하며 빈도가 높은 이벤트를 처리합니다.</p>
     */
    @Bean(name = "realtimeEventExecutor")
    public Executor realtimeEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 수 (빈도 높음)
        executor.setMaxPoolSize(15); // 최대 스레드 수
        executor.setQueueCapacity(100); // 대기열 크기 (버스트 트래픽 대응)
        executor.setThreadNamePrefix("realtime-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 캐시 갱신 전용 스레드 풀
     * <p>조회 시 HASH-ZSET 불일치 감지 후 비동기 HASH 갱신에 사용됩니다.</p>
     */
    @Bean(name = "cacheRefreshExecutor")
    public Executor cacheRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("cache-refresh-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 신고 전용 스레드 풀
     * <p>신고 저장 이벤트를 처리합니다.</p>
     * <p>빈도는 낮지만 데이터 무결성이 중요합니다.</p>
     */
    @Bean(name = "reportEventExecutor")
    public Executor reportEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); // 기본 스레드 수 (신고는 빈도 낮음)
        executor.setMaxPoolSize(3); // 최대 스레드 수
        executor.setQueueCapacity(20); // 대기열 크기
        executor.setThreadNamePrefix("report-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // 신고 처리는 완료 대기
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}