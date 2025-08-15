package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.admin.application.port.out.AdminQueryPort;
import jaeik.growfarm.domain.admin.entity.QReport;
import jaeik.growfarm.domain.admin.entity.Report;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>관리자 조회 어댑터</h2>
 * <p>
 * 신고(`Report`) 관련 데이터를 영속화하고 조회
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class AdminQueryAdapter implements AdminQueryPort {

    private final ReportRepository reportRepository;
    private final JPAQueryFactory queryFactory;


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
    @Override
    public Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable) {
        QReport report = QReport.report;
        BooleanExpression whereClause = (reportType != null) ? report.reportType.eq(reportType) : null;

        List<ReportDTO> content = queryFactory
                .select(Projections.bean(ReportDTO.class,
                        report.id,
                        report.reporter.id.as("reporterId"),
                        report.reporter.userName.as("reporterName"),
                        report.reportType,
                        report.targetId,
                        report.content,
                        report.createdAt))
                .from(report)
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

        return new PageImpl<>(content, pageable, count == null ? 0 : count);
    }


    /**
     * <h3>ID로 신고 조회</h3>
     * <p>주어진 신고 ID로 신고 엔티티를 조회합니다.</p>
     *
     * @param reportId 신고 ID
     * @return Optional<Report> 조회된 신고 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Report> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }
}
