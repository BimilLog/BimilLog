package jaeik.bimillog.infrastructure.adapter.in.admin.dto;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.user.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

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
     * <h3>POST/COMMENT 신고 시 targetId 필수 검증</h3>
     * <p>POST와 COMMENT 타입의 신고는 targetId가 반드시 필요합니다.</p>
     * <p>게시글 또는 댓글 신고 시 해당 ID를 통해 신고 대상을 식별해야 하기 때문입니다.</p>
     *
     * @return boolean POST/COMMENT 타입일 때 targetId가 null이 아니면 true, 그 외 타입은 항상 true
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "글, 댓글 신고는 신고대상이 필수입니다")
    public boolean isTargetIdRequiredForContentReport() {
        if (reportType == ReportType.POST || reportType == ReportType.COMMENT) {
            return targetId != null;
        }
        return true;
    }

    /**
     * <h3>ERROR/IMPROVEMENT 신고 시 targetId null 검증</h3>
     * <p>ERROR와 IMPROVEMENT 타입의 신고는 targetId가 필요하지 않습니다.</p>
     * <p>시스템 오류 신고나 개선 제안은 특정 게시글이나 댓글과 무관하기 때문입니다.</p>
     *
     * @return boolean ERROR/IMPROVEMENT 타입일 때 targetId가 null이면 true, 그 외 타입은 항상 true
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "에러, 개선 신고는 신고대상이 없어야 합니다")
    public boolean isTargetIdNotAllowedForSystemReport() {
        if (reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) {
            return targetId == null;
        }
        return true;
    }

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
        User reporter = report.getReporter();
        return ReportDTO.builder()
                .id(report.getId())
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterName(reporter != null ? reporter.getUserName() : "익명")
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
