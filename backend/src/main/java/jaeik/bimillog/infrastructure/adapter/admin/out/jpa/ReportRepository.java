package jaeik.bimillog.infrastructure.adapter.admin.out.jpa;

import jaeik.bimillog.domain.admin.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h2>신고 저장소</h2>
 * <p>
 * Spring Data JPA를 사용하여 신고(`Report`) 엔티티의 기본적인 CRUD 작업을 제공하는 저장소 인터페이스
 * </p>
 * <p>
 * 복잡한 쿼리는 AdminQueryAdapter에서 QueryDSL을 사용하여 처리됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ReportRepository extends JpaRepository<Report, Long> {
}
