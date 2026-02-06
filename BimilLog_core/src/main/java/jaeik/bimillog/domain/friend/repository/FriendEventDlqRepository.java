package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.jpa.FriendDlqStatus;
import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <h2>친구 이벤트 DLQ 레포지토리</h2>
 * <p>친구 이벤트 DLQ 엔티티의 CRUD 작업을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
public interface FriendEventDlqRepository extends JpaRepository<FriendEventDlq, Long> {

    /**
     * PENDING 상태의 이벤트를 생성 시간 순으로 조회합니다.
     *
     * @param status 조회할 상태
     * @param maxRetry 최대 재시도 횟수
     * @param limit 조회할 최대 개수
     * @return PENDING 상태의 이벤트 목록
     */
    @Query("SELECT e FROM FriendEventDlq e WHERE e.status = :status AND e.retryCount < :maxRetry ORDER BY e.createdAt ASC LIMIT :limit")
    List<FriendEventDlq> findPendingEvents(@Param("status") FriendDlqStatus status, @Param("maxRetry") int maxRetry, @Param("limit") int limit);
}
