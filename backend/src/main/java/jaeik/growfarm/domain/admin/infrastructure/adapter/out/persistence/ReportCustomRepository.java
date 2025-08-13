package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>신고 사용자 정의 레포지토리 인터페이스</h2>
 * <p>신고(`Report`) 엔티티의 복잡한 조회 작업을 위한 사용자 정의 레포지토리 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ReportCustomRepository {
    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>주어진 신고 유형에 따라 신고 목록을 페이지네이션하여 조회합니다.</p>
     *
     * @param reportType 신고 유형 (null 가능, 전체 조회 시)
     * @param pageable   페이지 정보
     * @return Page<ReportDTO> 신고 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable);
}
