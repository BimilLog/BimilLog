package jaeik.bimillog.domain.admin.application.port.out;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>AdminQueryPort</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 조회 작업을 위한 Secondary Port 인터페이스입니다.
 * </p>
 * <p>
 * CQRS 패턴에 따른 읽기 전용 작업을 담당하며, 도메인 계층이 인프라스트럭처에 직접 의존하지 않도록 합니다.
 * </p>
 * <p>
 * AdminQueryService에서 신고 목록 조회가 필요할 때 이 인터페이스를 호출하고,
 * AdminQueryAdapter에서 실제 JPA Repository를 통한 데이터베이스 조회를 구현합니다.
 * </p>
 * <p>
 * 읽기 최적화된 쿼리를 통해 관리자 대시보드에 필요한 데이터를 효율적으로 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminQueryPort {

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>AdminQueryService에서 관리자 대시보드의 신고 목록 표시를 위해 호출하는 메서드입니다.</p>
     * <p>관리자가 신고 관리 화면에서 특정 유형의 신고만 필터링하거나 전체 신고를 확인할 때 사용됩니다.</p>
     * <p>AdminQueryAdapter를 통해 ReportRepository의 JPA 쿼리로 실제 데이터베이스 조회가 수행됩니다.</p>
     * <p>페이지네이션과 정렬을 지원하여 대용량 신고 데이터도 효율적으로 처리할 수 있습니다.</p>
     *
     * @param reportType 필터링할 신고 유형 (null이면 전체 신고 조회)
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬 조건)
     * @return Page<Report> 페이지네이션된 신고 목록
     * @author Jaeik
     * @since 2.0.0
     */
    Page<Report> findReportsWithPaging(ReportType reportType, Pageable pageable);

}
