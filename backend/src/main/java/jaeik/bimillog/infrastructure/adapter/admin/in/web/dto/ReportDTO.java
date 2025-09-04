package jaeik.bimillog.infrastructure.adapter.admin.in.web.dto;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * <h2>신고 DTO</h2>
 * <p>웹 계층에서 신고 정보를 전송하기 위한 데이터 전송 객체</p>
 * <p>도메인 엔티티와 웹 요청/응답 사이의 변환을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private Long reporterId;
    private String reporterName;
    
    @NotNull(message = "신고 유형은 필수입니다")
    private ReportType reportType;
    
    private Long targetId;
    
    @NotBlank(message = "신고 내용은 필수입니다")
    @Size(min = 10, max = 500, message = "신고 내용은 10-500자 사이여야 합니다")
    private String content;
    
    private Instant createdAt;

    /**
     * <h3>Report 엔티티로부터 ReportDTO 생성</h3>
     * <p>도메인 엔티티를 DTO로 변환합니다.</p>
     *
     * @param report 신고 엔티티
     * @return ReportDTO 변환된 DTO 객체
     * @author Jaeik
     * @since 2.0.0
     */
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

}
