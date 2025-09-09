package jaeik.bimillog.infrastructure.adapter.admin.out.admin;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.admin.application.port.out.AdminQueryPort;
import jaeik.bimillog.domain.admin.entity.QReport;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>AdminQueryAdapter</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 조회 작업을 위한 Secondary Adapter 구현체입니다.
 * </p>
 * <p>
 * AdminQueryPort 인터페이스를 구현하여 CQRS 패턴의 읽기 측면을 담당하며, 
 * 관리자 대시보드에 필요한 신고 데이터를 효율적으로 조회합니다.
 * </p>
 * <p>
 * AdminQueryService에서 신고 목록 조회 요청을 받아 QueryDSL을 사용한 최적화된 쿼리를 실행합니다.
 * </p>
 * <p>
 * 동적 필터링, 페이지네이션, N+1 문제 해결을 위한 fetch join 등 성능 최적화 기법을 적용하여
 * 대용량 신고 데이터도 빠르게 처리할 수 있도록 설계되었습니다.
 * </p>
 * <p>
 * Repository 계층의 핵심 구성 요소로서 도메인과 데이터베이스 간의 효율적인 데이터 조회를 담당합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class AdminQueryAdapter implements AdminQueryPort {

    private final JPAQueryFactory queryFactory;

    /**
     * <h3>신고 목록 최적화된 페이지네이션 조회</h3>
     * <p>AdminQueryPort 인터페이스의 findReportsWithPaging 메서드를 구현하여 관리자 대시보드용 신고 데이터를 조회합니다.</p>
     * <p>AdminQueryService의 getReportList 메서드에서 관리자가 신고 관리 화면을 요청할 때 호출됩니다.</p>
     * <p>QueryDSL을 활용한 동적 쿼리로 신고 유형에 따른 필터링을 지원하고, 타입 안전성을 보장합니다.</p>
     * <p>N+1 문제 해결을 위해 신고자(reporter) 정보를 fetch join으로 함께 조회하여 성능을 최적화합니다.</p>
     * <p>최신 신고부터 내림차순으로 정렬하여 관리자가 긴급한 신고를 우선 확인할 수 있도록 합니다.</p>
     * <p>count 쿼리를 별도로 실행하여 전체 데이터 개수를 정확히 파악하고 페이지네이션 정보를 제공합니다.</p>
     *
     * @param reportType 필터링할 신고 유형 (null이면 전체 신고 조회)
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬 조건)
     * @return Page<Report> 페이지네이션된 신고 목록 (신고자 정보 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<Report> findReportsWithPaging(ReportType reportType, Pageable pageable) {
        QReport report = QReport.report;
        QUser user = QUser.user;
        BooleanExpression whereClause = (reportType != null) ? report.reportType.eq(reportType) : null;

        List<Report> reports = queryFactory
                .select(report)
                .from(report)
                .leftJoin(report.reporter, user).fetchJoin()
                .where(whereClause)
                .orderBy(report.createdAt.desc(), report.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(report.count())
                .from(report)
                .where(whereClause)
                .fetchOne();

        return new PageImpl<>(reports, pageable, count == null ? 0 : count);
    }
}
