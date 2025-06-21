package jaeik.growfarm.dto.admin;

import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <h2>신고 DTO</h2>
 * <p>
 * 신고 정보를 담는 DTO 클래스
 * </p>
 * <p>
 * 신고 ID, 신고 타입, 신고한 유저 ID, 신고 대상 ID, 신고 내용 등을 포함한다.
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long reportId;

    private ReportType reportType;

    private Long userId;

    private Long targetId;

    @Size(max = 500, message = "내용은 최대 500자 까지 입력 가능합니다.")
    @Size(min = 10, message = "내용은 최소 10자 이상 입력해야 합니다.")
    private String content;

    public static ReportDTO createReportDTO(Report report) {
        return ReportDTO.builder()
                .reportId(report.getId())
                .reportType(report.getReportType())
                .userId(report.getUsers().getId())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .build();
    }
}
