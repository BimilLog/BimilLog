package jaeik.growfarm.repository.admin;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * <h2>신고 Custom Repository 구현 클래스</h2>
 * <p>
 * 신고 관련 커스텀 데이터베이스 작업을 수행하는 Repository 구현 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface ReportCustomRepository {

    Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable);
}
