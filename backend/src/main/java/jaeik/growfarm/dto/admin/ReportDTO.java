package jaeik.growfarm.dto.admin;

import jaeik.growfarm.entity.report.ReportType;
import jakarta.validation.constraints.Size;
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

    @Size(max = 500, message = "내용은 최대 500자 까지 입력 가능합니다.")
    private String content;
}


