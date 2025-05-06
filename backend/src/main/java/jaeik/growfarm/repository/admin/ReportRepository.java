package jaeik.growfarm.repository.admin;

import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * 신고 Repository
 * 신고 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReportType(ReportType reportType, Pageable pageable);

}
