package jaeik.bimillog.infrastructure.resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <h2>DB Fallback Gateway</h2>
 * <p>Redis 폴백 시 모든 DB 요청을 단일 게이트웨이로 통합하여 DB를 보호합니다.</p>
 * <p>Semaphore Bulkhead로 동시 요청을 10개로 제한하고, Circuit Breaker로 연속 실패 시 차단합니다.</p>
 *
 * <h3>동작 방식:</h3>
 * <ul>
 *     <li>Bulkhead: HikariCP 30개 중 폴백용 10개로 제한, 나머지 20개는 정상 요청용</li>
 *     <li>Circuit Breaker: 실패율 50% 이상 시 30초 차단</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Component
@Slf4j
public class DbFallbackGateway {

    /**
     * <h3>목록 조회용 DB 폴백 실행</h3>
     * <p>Bulkhead + Circuit Breaker로 보호된 DB 조회를 실행합니다.</p>
     *
     * @param type     폴백 유형 (로깅용)
     * @param pageable 페이지 정보
     * @param dbQuery  DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 페이지
     */
    @Bulkhead(name = "dbFallback", fallbackMethod = "bulkheadFallback")
    @CircuitBreaker(name = "dbFallback", fallbackMethod = "circuitBreakerFallback")
    public Page<PostSimpleDetail> execute(
            FallbackType type,
            Pageable pageable,
            Supplier<Page<PostSimpleDetail>> dbQuery
    ) {
        log.info("[DB_FALLBACK] 시작 - type={}", type.getDescription());
        Page<PostSimpleDetail> result = dbQuery.get();
        log.info("[DB_FALLBACK] 완료 - type={}, count={}", type.getDescription(), result.getTotalElements());
        return result;
    }

    /**
     * <h3>상세 조회용 DB 폴백 실행</h3>
     * <p>Bulkhead + Circuit Breaker로 보호된 상세 조회를 실행합니다.</p>
     *
     * @param type    폴백 유형 (로깅용)
     * @param postId  게시글 ID (로깅용)
     * @param dbQuery DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 상세 (Optional)
     */
    @Bulkhead(name = "dbFallback", fallbackMethod = "bulkheadDetailFallback")
    @CircuitBreaker(name = "dbFallback", fallbackMethod = "circuitBreakerDetailFallback")
    public Optional<PostDetail> executeDetail(
            FallbackType type,
            Long postId,
            Supplier<Optional<PostDetail>> dbQuery
    ) {
        log.info("[DB_FALLBACK] 상세 조회 - type={}, postId={}", type.getDescription(), postId);
        Optional<PostDetail> result = dbQuery.get();
        log.info("[DB_FALLBACK] 상세 조회 완료 - type={}, postId={}, found={}", type.getDescription(), postId, result.isPresent());
        return result;
    }

    // ========== Bulkhead Fallback Methods ==========

    /**
     * <h3>목록 조회 Bulkhead 폴백</h3>
     * <p>동시 요청 초과 시 빈 Page를 반환합니다.</p>
     */
    @SuppressWarnings("unused")
    private Page<PostSimpleDetail> bulkheadFallback(
            FallbackType type,
            Pageable pageable,
            Supplier<Page<PostSimpleDetail>> dbQuery,
            io.github.resilience4j.bulkhead.BulkheadFullException e
    ) {
        log.warn("[DB_FALLBACK] Bulkhead 초과 - type={}, error={}", type.getDescription(), e.getMessage());
        return new PageImpl<>(List.of(), pageable, 0);
    }

    /**
     * <h3>상세 조회 Bulkhead 폴백</h3>
     * <p>동시 요청 초과 시 Optional.empty()를 반환합니다.</p>
     */
    @SuppressWarnings("unused")
    private Optional<PostDetail> bulkheadDetailFallback(
            FallbackType type,
            Long postId,
            Supplier<Optional<PostDetail>> dbQuery,
            io.github.resilience4j.bulkhead.BulkheadFullException e
    ) {
        log.warn("[DB_FALLBACK] Bulkhead 초과 (상세) - type={}, postId={}, error={}", type.getDescription(), postId, e.getMessage());
        return Optional.empty();
    }

    // ========== Circuit Breaker Fallback Methods ==========

    /**
     * <h3>목록 조회 Circuit Breaker 폴백</h3>
     * <p>Circuit OPEN 상태 시 빈 Page를 반환합니다.</p>
     */
    @SuppressWarnings("unused")
    private Page<PostSimpleDetail> circuitBreakerFallback(
            FallbackType type,
            Pageable pageable,
            Supplier<Page<PostSimpleDetail>> dbQuery,
            Throwable e
    ) {
        log.error("[DB_FALLBACK] Circuit Breaker OPEN - type={}, error={}", type.getDescription(), e.getMessage());
        return new PageImpl<>(List.of(), pageable, 0);
    }

    /**
     * <h3>상세 조회 Circuit Breaker 폴백</h3>
     * <p>Circuit OPEN 상태 시 Optional.empty()를 반환합니다.</p>
     */
    @SuppressWarnings("unused")
    private Optional<PostDetail> circuitBreakerDetailFallback(
            FallbackType type,
            Long postId,
            Supplier<Optional<PostDetail>> dbQuery,
            Throwable e
    ) {
        log.error("[DB_FALLBACK] Circuit Breaker OPEN (상세) - type={}, postId={}, error={}", type.getDescription(), postId, e.getMessage());
        return Optional.empty();
    }
}
