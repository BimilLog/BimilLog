package jaeik.bimillog.infrastructure.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * <h2>DB Fallback Gateway</h2>
 * <p>모든 DB 읽기 요청을 단일 게이트웨이로 통합하여 애플리케이션과 DB를 보호합니다.</p>
 *
 * <h3>3중 보호 체계:</h3>
 * <ul>
 *     <li>ThreadPool Bulkhead: DB 쿼리를 별도 스레드풀에서 실행하여 Tomcat 스레드 격리 (core 5, max 15)</li>
 *     <li>TimeLimiter: 쿼리당 최대 5초 타임아웃으로 스레드 점유 시간 제한</li>
 *     <li>Circuit Breaker: DB 장애 감지 시 즉시 차단 (실패율 80% 이상 → 30초 OPEN)</li>
 * </ul>
 *
 * <h3>데코레이션 순서 (innermost → outermost):</h3>
 * <p>{@code CircuitBreaker → ThreadPoolBulkhead → TimeLimiter}</p>
 * <p>CircuitBreaker가 DB 쿼리에 가장 가깝게 위치하여 원본 DB 예외를 직접 감지합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@Slf4j
public class DbFallbackGateway {

    private final CircuitBreaker circuitBreaker;
    private final ThreadPoolBulkhead bulkhead;
    private final TimeLimiter timeLimiter;

    public DbFallbackGateway(
            CircuitBreakerRegistry circuitBreakerRegistry,
            ThreadPoolBulkheadRegistry bulkheadRegistry,
            TimeLimiterRegistry timeLimiterRegistry
    ) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("dbFallback");
        this.bulkhead = bulkheadRegistry.bulkhead("dbFallback");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("dbFallback");
    }

    /**
     * <h3>목록 조회용 DB 실행</h3>
     *
     * @param type     폴백 유형 (로깅용)
     * @param pageable 페이지 정보
     * @param dbQuery  DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 페이지 (실패 시 빈 Page)
     */
    public Page<PostSimpleDetail> execute(
            FallbackType type,
            Pageable pageable,
            Supplier<Page<PostSimpleDetail>> dbQuery
    ) {
        try {
            Supplier<Page<PostSimpleDetail>> cbProtected =
                    CircuitBreaker.decorateSupplier(circuitBreaker, dbQuery);

            Supplier<CompletionStage<Page<PostSimpleDetail>>> bulkheadProtected =
                    ThreadPoolBulkhead.decorateSupplier(bulkhead, cbProtected);

            return timeLimiter.executeFutureSupplier(
                    () -> bulkheadProtected.get().toCompletableFuture());
        } catch (BulkheadFullException e) {
            log.warn("[DB_BULKHEAD] 스레드풀 포화 (목록 조회) - type={}", type.getDescription());
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (TimeoutException e) {
            log.warn("[DB_TIMELIMIT] 타임아웃 (목록 조회) - type={}", type.getDescription());
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (ExecutionException e) {
            logExecutionFailure("목록 조회", type.getDescription(), e.getCause());
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (Exception e) {
            log.warn("[DB_GATEWAY] 실패 (목록 조회) - type={}, error={}", type.getDescription(), e.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    /**
     * <h3>상세 조회용 DB 실행</h3>
     *
     * @param type    폴백 유형 (로깅용)
     * @param postId  게시글 ID (로깅용)
     * @param dbQuery DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 상세 (실패 시 Optional.empty())
     */
    public Optional<PostDetail> executeDetail(
            FallbackType type,
            Long postId,
            Supplier<Optional<PostDetail>> dbQuery
    ) {
        try {
            Supplier<Optional<PostDetail>> cbProtected =
                    CircuitBreaker.decorateSupplier(circuitBreaker, dbQuery);

            Supplier<CompletionStage<Optional<PostDetail>>> bulkheadProtected =
                    ThreadPoolBulkhead.decorateSupplier(bulkhead, cbProtected);

            return timeLimiter.executeFutureSupplier(
                    () -> bulkheadProtected.get().toCompletableFuture());
        } catch (BulkheadFullException e) {
            log.warn("[DB_BULKHEAD] 스레드풀 포화 (상세 조회, postId={}) - type={}", postId, type.getDescription());
            return Optional.empty();
        } catch (TimeoutException e) {
            log.warn("[DB_TIMELIMIT] 타임아웃 (상세 조회, postId={}) - type={}", postId, type.getDescription());
            return Optional.empty();
        } catch (ExecutionException e) {
            logExecutionFailure("상세 조회, postId=" + postId, type.getDescription(), e.getCause());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[DB_GATEWAY] 실패 (상세 조회, postId={}) - type={}, error={}", postId, type.getDescription(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * <h3>ID 목록 기반 조회용 DB 실행</h3>
     * <p>실시간 인기글의 Redis 서킷 열림 시 DB 폴백에 사용됩니다.</p>
     *
     * @param type    폴백 유형 (로깅용)
     * @param postIds 조회할 게시글 ID 목록 (로깅용)
     * @param dbQuery DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 목록 (실패 시 빈 List)
     */
    public List<PostSimpleDetail> executeList(
            FallbackType type,
            List<Long> postIds,
            Supplier<List<PostSimpleDetail>> dbQuery
    ) {
        try {
            Supplier<List<PostSimpleDetail>> cbProtected =
                    CircuitBreaker.decorateSupplier(circuitBreaker, dbQuery);

            Supplier<CompletionStage<List<PostSimpleDetail>>> bulkheadProtected =
                    ThreadPoolBulkhead.decorateSupplier(bulkhead, cbProtected);

            return timeLimiter.executeFutureSupplier(
                    () -> bulkheadProtected.get().toCompletableFuture());
        } catch (BulkheadFullException e) {
            log.warn("[DB_BULKHEAD] 스레드풀 포화 (ID 목록 조회, count={}) - type={}", postIds.size(), type.getDescription());
            return List.of();
        } catch (TimeoutException e) {
            log.warn("[DB_TIMELIMIT] 타임아웃 (ID 목록 조회, count={}) - type={}", postIds.size(), type.getDescription());
            return List.of();
        } catch (ExecutionException e) {
            logExecutionFailure("ID 목록 조회, count=" + postIds.size(), type.getDescription(), e.getCause());
            return List.of();
        } catch (Exception e) {
            log.warn("[DB_GATEWAY] 실패 (ID 목록 조회, count={}) - type={}, error={}", postIds.size(), type.getDescription(), e.getMessage());
            return List.of();
        }
    }

    // ========== Private Utility ==========

    private void logExecutionFailure(String operation, String typeDescription, Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            log.warn("[DB_CIRCUIT] 서킷 오픈 ({}) - type={}", operation, typeDescription);
        } else {
            log.error("[DB_GATEWAY] DB 오류 ({}) - type={}, error={}", operation, typeDescription,
                    cause != null ? cause.getMessage() : "unknown");
        }
    }
}
