package jaeik.growfarm.repository.admin;

import jaeik.growfarm.entity.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>신고 Repository</h2>
 * <p>
 * 신고 관련 데이터베이스 작업을 수행하는 Repository
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> , ReportCustomRepository {

}
