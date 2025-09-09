
package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminQueryPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>AdminQueryService</h2>
 * <p>
 * 관리자 조회 관련 UseCase 인터페이스의 구체적 구현체로서 읽기 전용 비즈니스 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 조회 처리를 담당하며, CQRS 패턴에 따른 읽기 전용 작업을 수행합니다.
 * </p>
 * <p>
 * AdminQueryController에서 관리자 대시보드의 신고 목록 조회 요청을 받아 처리하고,
 * 페이지네이션과 필터링을 적용하여 관리자가 효율적으로 신고를 검토할 수 있도록 합니다.
 * </p>
 * <p>
 * 읽기 전용 트랜잭션을 사용하여 성능을 최적화하고, AdminQueryPort를 통해 데이터베이스 조회를 위임합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminQueryService implements AdminQueryUseCase {

    private final AdminQueryPort adminQueryPort;

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>AdminQueryUseCase 인터페이스의 신고 목록 조회 기능을 구현하며, 관리자 대시보드의 신고 관리 화면에 데이터를 제공합니다.</p>
     * <p>AdminQueryController에서 관리자가 신고 목록을 확인하거나 특정 유형의 신고만 필터링하여 조회할 때 호출됩니다.</p>
     * <p>최신 신고부터 내림차순으로 정렬하여 관리자가 긴급한 신고를 먼저 확인할 수 있도록 합니다.</p>
     * <p>신고 유형별 필터링을 지원하여 관리자가 POST 신고, COMMENT 신고, 시스템 오류 등을 구분하여 처리할 수 있습니다.</p>
     * <p>Spring Data JPA의 Pageable을 활용하여 대용량 신고 데이터도 효율적으로 처리합니다.</p>
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 표시할 신고 수
     * @param reportType 필터링할 신고 유형 (null이면 전체 조회)
     * @return Page<Report> 페이지네이션된 신고 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<Report> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminQueryPort.findReportsWithPaging(reportType, pageable);
    }
}
