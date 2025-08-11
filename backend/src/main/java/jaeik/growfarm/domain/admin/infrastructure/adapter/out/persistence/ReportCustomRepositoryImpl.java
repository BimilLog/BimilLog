package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.admin.domain.QReport;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>신고 Custom Repository 구현 클래스</h2>
 * <p>
 * 신고 관련 커스텀 데이터베이스 작업을 수행하는 Repository 구현 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class ReportCustomRepositoryImpl implements ReportCustomRepository {

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
}
