package jaeik.bimillog.infrastructure.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <h2>공통 비동기 처리 설정</h2>
 * <p>회원 이벤트, 신고 등 도메인 횡단 스레드 풀을 정의합니다.</p>
 * <p>{@code @EnableAsync}, {@code @EnableRetry}를 활성화합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 * @see PostAsyncConfig
 * @see FriendAsyncConfig
 * @see NotificationAsyncConfig
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {

    /**
     * 멤버 로그아웃, 회원탈퇴, 밴 전용 스레드 풀
     * <p>회원 상태 변경 시 발생하는 정리 작업을 처리합니다.</p>
     * <p>SSE 연결 해제, 토큰 삭제, 소셜 계정 연동 해제 등의 작업을 포함합니다.</p>
     */
    @Bean(name = "memberEventExecutor")
    public Executor memberEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("member-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
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
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("report-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
