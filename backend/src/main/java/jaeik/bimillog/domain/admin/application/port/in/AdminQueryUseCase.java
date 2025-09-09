package jaeik.bimillog.domain.admin.application.port.in;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import org.springframework.data.domain.Page;

/**
 * <h2>AdminQueryUseCase</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 조회 처리를 위한 Primary Port 인터페이스입니다.
 * </p>
 * <p>
 * CQRS 패턴에 따른 쿼리(Query) 측면의 관리자 기능을 정의하며, 신고 목록 조회, 통계 확인 등
 * 데이터를 읽기만 하는 관리자 업무를 추상화합니다.
 * </p>
 * <p>
 * AdminQueryController에서 관리자 대시보드 요청을 받아 이 인터페이스를 호출하고,
 * AdminQueryService에서 읽기 전용 비즈니스 로직을 구현합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminQueryUseCase {

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>관리자 대시보드에서 신고 목록을 효율적으로 조회하고 관리할 수 있도록 지원합니다.</p>
     * <p>AdminQueryController에서 관리자의 신고 관리 화면 요청을 받아 이 메서드를 호출합니다.</p>
     * <p>신고 유형별 필터링과 페이지네이션을 통해 대용량 신고 데이터를 관리자가 효과적으로 처리할 수 있습니다.</p>
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 표시할 신고 수
     * @param reportType 필터링할 신고 유형 (null이면 전체 조회)
     * @return Page<Report> 페이지네이션된 신고 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<Report> getReportList(int page, int size, ReportType reportType);
}
