package jaeik.growfarm.repository.admin;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.QReport;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private final JPAQueryFactory jpaQueryFactory;

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

        BooleanBuilder builder = new BooleanBuilder();
        if (reportType != null) {
            builder.and(report.reportType.eq(reportType));
        }

        JPAQuery<ReportDTO> query = jpaQueryFactory
                .select(Projections.bean(ReportDTO.class,
                        report.id.as("reportId"),
                        report.reportType,
                        report.users.id.as("userId"),
                        report.targetId,
                        report.content))
                .from(report)
                .where(builder);

        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                PathBuilder<Report> pathBuilder = new PathBuilder<>(Report.class, "report");
                query.orderBy(new OrderSpecifier<>(
                        order.isAscending() ? Order.ASC : Order.DESC,
                        pathBuilder.get(order.getProperty(), Comparable.class)));
            }
        } else {
            query.orderBy(report.createdAt.desc());
        }

        List<ReportDTO> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(report.count())
                .from(report)
                .where(builder)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }
}
