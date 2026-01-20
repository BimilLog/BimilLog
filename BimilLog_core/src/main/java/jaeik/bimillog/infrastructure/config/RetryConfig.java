package jaeik.bimillog.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

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
 *
 * @author Jaeik
 * @since 2.5.0
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
