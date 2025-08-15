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
 * <h2>신고 영속성 어댑터</h2>
 * <p>
 * 신고(`Report`) 관련 데이터를 영속화하고 조회하는 Outgoing-Adapter
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
     * <h3>신고 목록 페이징 조회</h3>
     * <p>
     * 신고 타입에 따라 필터링하여 신고 목록을 페이징 조회하고 ReportDTO로 변환하여 반환합니다.
     * </p>
     * <p>
     * 신고 타입이 null인 경우 전체 신고를 조회합니다.
     * </p>
     * <p>
     * 기본적으로 최신순(생성일시 내림차순)으로 조회합니다.
     * </p>
     *
     * @param reportType 신고 타입 (null이면 전체 조회)
     * @param pageable   페이징 정보 객체
     * @return Page<ReportDTO> 신고 DTO 페이지 객체
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


    @Override
    public Optional<Report> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }
}
