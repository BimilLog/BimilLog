package jaeik.bimillog.infrastructure.adapter.admin.out.persistence;

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
 * <h2>관리자 조회 어댑터</h2>
 * <p>
 * 헥사고날 아키텍처의 Secondary Adapter (Driven Adapter)
 * 관리자 도메인의 AdminQueryPort를 구현하여 신고(`Report`) 관련 데이터를 영속화하고 조회하는 역할
 * </p>
 * <p>
 * QueryDSL을 사용하여 동적 쿼리와 페이지네이션을 지원합니다.
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
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>주어진 신고 유형에 따라 신고 목록을 페이지네이션하여 조회합니다.</p>
     * <p>QueryDSL을 사용하여 동적 쿼리를 생성하고, 최신 신고 순으로 정렬합니다.</p>
     * <p>N+1 문제 방지를 위해 User 연관관계를 fetch join으로 함께 조회합니다.</p>
     *
     * @param reportType 신고 유형 (null 가능, 전체 조회 시)
     * @param pageable   페이지 정보 (정렬, 페이지 크기, 오프셋 포함)
     * @return Page<Report> 신고 엔티티 페이지
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
