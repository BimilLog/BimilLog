package jaeik.growfarm.infrastructure.adapter.admin.in.web.dto;

import jaeik.growfarm.domain.admin.entity.Report;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.admin.entity.ReportVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private ReportType reportType;
    private Long targetId;
    private String content;
    private Instant createdAt;

    public static ReportDTO from(Report report) {
        return ReportDTO.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getUserName())
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /**
     * <h3>ReportVO로 변환</h3>
     * <p>ReportDTO를 도메인 순수 값 객체인 ReportVO로 변환합니다.</p>
     *
     * @return ReportVO 도메인 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public ReportVO toReportVO() {
        return ReportVO.of(reportType, targetId, content);
    }
}
