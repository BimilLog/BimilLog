package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.post.entity.jpa.PostReadModelDlq;
import jaeik.bimillog.domain.post.entity.jpa.PostReadModelDlq.DlqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <h2>Post Read Model DLQ Repository</h2>
 * <p>PostReadModel DLQ 엔티티의 CRUD 작업을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
public interface PostReadModelDlqRepository extends JpaRepository<PostReadModelDlq, Long> {

    /**
     * PENDING 상태의 이벤트를 생성 시간 순으로 조회합니다.
     *
     * @param status   조회할 상태
     * @param maxRetry 최대 재시도 횟수
     * @param limit    조회할 최대 개수
     * @return PENDING 상태의 이벤트 목록
     */
    @Query("SELECT e FROM PostReadModelDlq e WHERE e.status = :status AND e.retryCount < :maxRetry ORDER BY e.createdAt ASC LIMIT :limit")
    List<PostReadModelDlq> findPendingEvents(
            @Param("status") DlqStatus status,
            @Param("maxRetry") int maxRetry,
            @Param("limit") int limit);

    /**
     * PENDING 상태의 이벤트를 기본 설정으로 조회합니다.
     *
     * @param limit 조회할 최대 개수
     * @return PENDING 상태의 이벤트 목록
     */
    default List<PostReadModelDlq> findPendingEvents(int limit) {
        return findPendingEvents(DlqStatus.PENDING, 3, limit);
    }
}
