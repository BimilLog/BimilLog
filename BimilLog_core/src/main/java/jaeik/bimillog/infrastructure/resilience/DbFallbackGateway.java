package jaeik.bimillog.infrastructure.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
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
 * <p>모든 DB 요청을 단일 게이트웨이로 통합하여 DB를 보호합니다.</p>
 * <p>Bulkhead로 동시 호출 수를 제한하고, Circuit Breaker로 연속 실패 시 차단합니다.</p>
 *
 * <h3>동작 방식:</h3>
 * <ul>
 *     <li>Bulkhead: 동시 최대 15개 호출 허용 (Semaphore 방식)</li>
 *     <li>Circuit Breaker: 최근 15개 중 최소 10개 검사, 실패율 80% 이상 시 30초 차단</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@Slf4j
public class DbFallbackGateway {

    /**
     * <h3>목록 조회용 DB 폴백 실행</h3>
     * <p>Circuit Breaker로 보호된 DB 조회를 실행합니다.</p>
     *
     * @param type     폴백 유형 (로깅용)
     * @param pageable 페이지 정보
     * @param dbQuery  DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 페이지
     */
    @CircuitBreaker(name = "dbFallback", fallbackMethod = "circuitBreakerFallback")
    @Bulkhead(name = "dbFallback")
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
     * <h3>상세 조회용 DB 실행</h3>
     * <p>Circuit Breaker로 보호된 상세 조회를 실행합니다.</p>
     *
     * @param type    폴백 유형 (로깅용)
     * @param postId  게시글 ID (로깅용)
     * @param dbQuery DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 상세 (Optional)
     */
    @CircuitBreaker(name = "dbFallback", fallbackMethod = "circuitBreakerDetailFallback")
    @Bulkhead(name = "dbFallback")
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

    /**
     * <h3>ID 목록 기반 조회용 DB 폴백 실행</h3>
     * <p>Circuit Breaker로 보호된 ID 목록 기반 조회를 실행합니다.</p>
     * <p>실시간 인기글의 Redis 서킷 열림 시 DB 폴백에 사용됩니다.</p>
     *
     * @param type    폴백 유형 (로깅용)
     * @param postIds 조회할 게시글 ID 목록 (로깅용)
     * @param dbQuery DB 조회 쿼리 (Supplier)
     * @return 조회된 게시글 목록
     */
    @CircuitBreaker(name = "dbFallback", fallbackMethod = "circuitBreakerListFallback")
    @Bulkhead(name = "dbFallback")
    public List<PostSimpleDetail> executeList(
            FallbackType type,
            List<Long> postIds,
            Supplier<List<PostSimpleDetail>> dbQuery
    ) {
        log.info("[DB_FALLBACK] ID 목록 조회 - type={}, count={}", type.getDescription(), postIds.size());
        List<PostSimpleDetail> result = dbQuery.get();
        log.info("[DB_FALLBACK] ID 목록 조회 완료 - type={}, count={}", type.getDescription(), result.size());
        return result;
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
        logFallbackReason("목록 조회", type.getDescription(), e);
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
        logFallbackReason("상세 조회, postId=" + postId, type.getDescription(), e);
        return Optional.empty();
    }

    /**
     * <h3>ID 목록 조회 Circuit Breaker 폴백</h3>
     * <p>Circuit OPEN 상태 시 빈 List를 반환합니다.</p>
     */
    @SuppressWarnings("unused")
    private List<PostSimpleDetail> circuitBreakerListFallback(
            FallbackType type,
            List<Long> postIds,
            Supplier<List<PostSimpleDetail>> dbQuery,
            Throwable e
    ) {
        logFallbackReason("ID 목록 조회, count=" + postIds.size(), type.getDescription(), e);
        return List.of();
    }

    // ========== Private Utility ==========

    private void logFallbackReason(String operation, String typeDescription, Throwable e) {
        if (e instanceof BulkheadFullException) {
            log.warn("[DB_FALLBACK] Bulkhead FULL ({}) - type={}", operation, typeDescription);
        } else {
            log.error("[DB_FALLBACK] Circuit Breaker OPEN ({}) - type={}, error={}", operation, typeDescription, e.getMessage());
        }
    }
}
