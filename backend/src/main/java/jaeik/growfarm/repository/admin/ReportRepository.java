package jaeik.growfarm.repository.admin;

import jaeik.growfarm.domain.admin.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h2>신고 Repository</h2>
 * <p>
 * 신고 관련 데이터베이스 작업을 수행하는 Repository
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ReportRepository extends JpaRepository<Report, Long>, ReportCustomRepository {
}
