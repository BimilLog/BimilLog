package jaeik.growfarm.domain.admin.entity;

import lombok.Builder;

import java.time.Instant;

/**
 * <h3>신고 요약 정보 값 객체</h3>
 * <p>
 * 신고 목록 조회 결과를 담는 도메인 순수 값 객체
 * ReportDTO의 도메인 전용 대체
 * </p>
 *
 * @param id 신고 ID
 * @param reporterId 신고자 ID
 * @param reporterName 신고자 이름
 * @param reportType 신고 유형
 * @param targetId 신고 대상 ID
 * @param content 신고 내용
 * @param createdAt 신고 생성일시
 * @author Jaeik
 * @since 2.0.0
 */
public record ReportSummary(
        Long id,
        Long reporterId,
        String reporterName,
        ReportType reportType,
        Long targetId,
        String content,
        Instant createdAt
) {

    @Builder
    public ReportSummary {
    }

    /**
     * <h3>신고 요약 정보 생성</h3>
     * <p>신고 엔티티로부터 요약 정보를 생성합니다.</p>
     *
     * @param report 신고 엔티티
     * @return ReportSummary 값 객체
     */
    public static ReportSummary from(Report report) {
        return ReportSummary.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getUserName())
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .build();
    }
}