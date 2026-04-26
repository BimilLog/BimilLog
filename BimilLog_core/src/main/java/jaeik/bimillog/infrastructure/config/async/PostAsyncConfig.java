package jaeik.bimillog.infrastructure.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <h2>게시글 관련 비동기 스레드 풀 설정</h2>
 * <p>실시간 인기글 점수, 카운트 버퍼, 캐시 갱신, 서킷 동기화 스레드 풀을 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Configuration
public class PostAsyncConfig {

//    /**
//     * 실시간 인기글 실시간 롤링페이퍼 점수 스레드 풀
//     * <p>Redis 기반의 실시간 점수 업데이트를 처리합니다.</p>
//     * <p>빠른 응답이 필요하며 빈도가 높은 이벤트를 처리합니다.</p>
//     */
//    @Bean(name = "realtimeEventExecutor")
//    public Executor realtimeEventExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(3);
//        executor.setMaxPoolSize(6);
//        executor.setQueueCapacity(10);
//        executor.setThreadNamePrefix("realtime-event-");
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setAwaitTerminationSeconds(30);
//        executor.initialize();
//        return executor;
//    }

//    /**
//     * 조회수 추천수 댓글수 관련 레디스 전송
//     */
//    @Bean(name = "cacheCountUpdateExecutor")
//    public Executor cacheCountUpdateExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(4);
//        executor.setMaxPoolSize(9);
//        executor.setQueueCapacity(12);
//        executor.setThreadNamePrefix("cache-count-");
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setAwaitTerminationSeconds(30);
//        executor.initialize();
//        return executor;
//    }

//    /**
//     * 캐시 갱신 전용 스레드 풀
//     * <p>글 작성/수정/삭제 시 JSON LIST 비동기 갱신에 사용됩니다.</p>
//     */
//    @Bean(name = "cacheRefreshExecutor")
//    public Executor cacheRefreshExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(4);
//        executor.setMaxPoolSize(8);
//        executor.setQueueCapacity(12);
//        executor.setThreadNamePrefix("cache-refresh-");
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setAwaitTerminationSeconds(30);
//        executor.initialize();
//        return executor;
//    }

//    /**
//     * 서킷브레이커 동기화 전용 스레드 풀
//     * <p>서킷 CLOSED 전환 시 Caffeine -> Redis 동기화에 사용됩니다.</p>
//     * <p>캐시 갱신 스레드 풀과 분리하여 동기화 작업이 일반 캐시 갱신을 블로킹하지 않습니다.</p>
//     */
//    @Bean(name = "circuitSyncExecutor")
//    public Executor circuitSyncExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(1);
//        executor.setMaxPoolSize(2);
//        executor.setQueueCapacity(4);
//        executor.setThreadNamePrefix("circuit-sync-");
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setAwaitTerminationSeconds(30);
//        executor.initialize();
//        return executor;
//    }
}
