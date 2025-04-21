package jaeik.growfarm.repository;

import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReportType(ReportType reportType, Pageable pageable);
}
