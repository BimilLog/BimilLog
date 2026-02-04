package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.post.entity.jpa.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * <h2>처리된 이벤트 Repository</h2>
 * <p>이벤트 멱등성 보장을 위한 처리된 이벤트 ID 저장소입니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    /**
     * 지정된 날짜 이전의 처리된 이벤트를 삭제합니다.
     *
     * @param cutoffDate 삭제 기준 날짜
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM ProcessedEvent e WHERE e.processedAt < :cutoffDate")
    int deleteByProcessedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
