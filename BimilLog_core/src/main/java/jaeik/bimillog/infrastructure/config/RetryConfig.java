package jaeik.bimillog.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * <h2>재시도 정책 설정</h2>
 * <p>
 * 이벤트 리스너의 재시도 정책을 정의합니다.
 * 일시적인 장애(DB 연결 실패, Redis 장애 등)에 대해 자동으로 재시도합니다.
 * </p>
 *
 * <h3>재시도 대상 예외</h3>
 * <ul>
 *     <li><b>TransientDataAccessException</b> - 일시적인 DB 접근 오류 (네트워크 장애 등)</li>
 *     <li><b>DataAccessResourceFailureException</b> - DB 리소스 획득 실패 (커넥션 풀 고갈 등)</li>
 *     <li><b>PessimisticLockingFailureException</b> - 비관적 락 획득 실패 (동시성 충돌)</li>
 *     <li><b>OptimisticLockingFailureException</b> - 낙관적 락 검증 실패 (버전 충돌)</li>
 *     <li><b>QueryTimeoutException</b> - 쿼리 타임아웃</li>
 *     <li><b>RedisConnectionFailureException</b> - Redis 연결 실패</li>
 * </ul>
 *
 * <h3>재시도 정책</h3>
 * <ul>
 *     <li><b>maxAttempts</b>: 3회 (초기 시도 1회 + 재시도 2회)</li>
 *     <li><b>backoff delay</b>: 1초 시작</li>
 *     <li><b>backoff multiplier</b>: 2배 (1초 → 2초 → 4초)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * &#64;EventListener
 * &#64;Async
 * &#64;Retryable(
 *     retryFor = {
 *         TransientDataAccessException.class,
 *         DataAccessResourceFailureException.class,
 *         RedisConnectionFailureException.class
 *     },
 *     maxAttempts = 3,
 *     backoff = &#64;Backoff(delay = 1000, multiplier = 2)
 * )
 * public void handleEvent(Event event) {
 *     // 이벤트 처리 로직
 * }
 * </pre>
 *
 * @author Jaeik
 * @since 2.5.0
 */
@Configuration
public class RetryConfig {

    /**
     * 기본 재시도 횟수
     */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    /**
     * 기본 백오프 지연 시간 (밀리초)
     */
    public static final long DEFAULT_BACKOFF_DELAY = 1000L;

    /**
     * 기본 백오프 승수
     */
    public static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
}
