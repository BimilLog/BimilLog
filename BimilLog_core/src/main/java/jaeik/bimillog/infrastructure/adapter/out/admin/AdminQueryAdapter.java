package jaeik.bimillog.infrastructure.adapter.out.admin;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.admin.service.AdminQueryService;
import jaeik.bimillog.domain.admin.entity.QReport;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>관리자 조회 어댑터</h2>
 * <p>관리자 도메인의 조회 작업을 담당하는 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class AdminQueryAdapter {

    private final JPAQueryFactory queryFactory;
    private final ReportRepository reportRepository;

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>관리자 대시보드용 신고 데이터를 조회합니다.</p>
     * <p>{@link AdminQueryService}에서 관리자 대시보드 신고 목록 조회 시 호출됩니다.</p>
     *
     * @param reportType 필터링할 신고 유형 (null이면 전체 신고 조회)
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬 조건)
     * @return Page<Report> 페이지네이션된 신고 목록 (신고자 정보 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public Page<Report> findReportsWithPaging(ReportType reportType, Pageable pageable) {
        QReport report = QReport.report;
        QMember member = QMember.member;
        BooleanExpression whereClause = (reportType == null) ? null : report.reportType.eq(reportType);

        List<Report> reports = queryFactory
                .select(report)
                .from(report)
                .leftJoin(report.reporter, member).fetchJoin()
                .where(whereClause)
                .orderBy(report.createdAt.desc())
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

    public List<Report> findAllReportsByUserId(Long memberId) {
        return reportRepository.findAllByReporterId(memberId);
    }
}
