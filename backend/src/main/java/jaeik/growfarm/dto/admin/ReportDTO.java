package jaeik.growfarm.dto.admin;

import jaeik.growfarm.domain.report.domain.Report;
import jaeik.growfarm.domain.report.domain.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private Long userId;
    private String userName;
    private ReportType reportType;
    private Long targetId;
    private String content;
    private Instant createdAt;

    public static ReportDTO from(Report report) {
        return ReportDTO.builder()
                .id(report.getId())
                .userId(report.getUsers().getId())
                .userName(report.getUsers().getUserName())
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
