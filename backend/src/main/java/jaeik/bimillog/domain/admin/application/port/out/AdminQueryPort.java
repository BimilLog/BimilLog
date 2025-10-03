package jaeik.bimillog.domain.admin.application.port.out;

import jaeik.bimillog.domain.admin.application.service.AdminQueryService;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>관리자 조회 포트</h2>
 * <p>관리자 도메인의 조회 작업을 담당하는 포트입니다.</p>
 * <p>신고 목록 페이지네이션 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminQueryPort {

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>신고 목록을 페이지네이션 최신순으로 조회합니다.</p>
     * <p>{@link AdminQueryService}에서 관리자 대시보드 신고 목록 조회 시 호출합니다.</p>
     *
     * @param reportType 필터링할 신고 유형 (null이면 전체 신고 조회)
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬 조건)
     * @return Page<Report> 페이지네이션된 신고 목록
     * @author Jaeik
     * @since 2.0.0
     */
    Page<Report> findReportsWithPaging(ReportType reportType, Pageable pageable);

    /**
     * <h3>특정 사용자의 신고 조회</h3>
     * <p>회원 탈퇴 시 reporter 연관을 제거하기 위해 신고 엔티티를 로딩합니다.</p>
     *
     * @param memberId 신고를 조회할 사용자 ID
     * @return java.util.List<Report> 해당 사용자가 작성한 신고 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Report> findAllReportsByUserId(Long memberId);


}
