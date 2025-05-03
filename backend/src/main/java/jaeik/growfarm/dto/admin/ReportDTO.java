package jaeik.growfarm.dto.admin;

import jaeik.growfarm.entity.report.ReportType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// 신고 DTO
@Getter @Setter
@Builder
public class ReportDTO {

    private Long reportId;

    private ReportType reportType;

    private Long userId;

    private Long targetId;

    private String content;
}


