package jaeik.bimillog.domain.admin.dto;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h2>신고 생성 DTO</h2>
 * <p>신고/건의사항 제출 요청에 사용되는 데이터 전송 객체</p>
 * <p>신고 타입에 따른 targetId 교차 검증 포함</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportDTO {

    private Long reporterId;
    private String reporterName;

    @NotNull(message = "신고 유형은 필수입니다")
    private ReportType reportType;

    private Long targetId;

    @NotBlank(message = "신고 내용은 필수입니다")
    @Size(min = 10, max = 500, message = "신고 내용은 10-500자 사이여야 합니다")
    private String content;

    /**
     * <h3>POST/COMMENT 신고 시 targetId 필수 검증</h3>
     * <p>POST와 COMMENT 타입의 신고는 targetId가 반드시 필요합니다.</p>
     *
     * @return boolean POST/COMMENT 타입일 때 targetId가 null이 아니면 true, 그 외 타입은 항상 true
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
     *
     * @return boolean ERROR/IMPROVEMENT 타입일 때 targetId가 null이면 true, 그 외 타입은 항상 true
     */
    @AssertTrue(message = "에러, 개선 신고는 신고대상이 없어야 합니다")
    public boolean isTargetIdNotAllowedForSystemReport() {
        if (reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) {
            return targetId == null;
        }
        return true;
    }
}
