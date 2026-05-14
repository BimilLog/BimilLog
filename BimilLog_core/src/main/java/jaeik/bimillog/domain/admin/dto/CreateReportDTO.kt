package jaeik.bimillog.domain.admin.dto

import jaeik.bimillog.domain.admin.entity.ReportType
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateReportDTO(

    val reporterId: Long?,
    val reporterName: String?,

    @field:NotNull(message = "신고 유형은 필수입니다")
    val reportType: ReportType,

    val targetId: Long?,

    @field:NotBlank(message = "신고 내용은 필수입니다")
    @field:Size(min = 10, max = 500, message = "신고 내용은 10-500자 사이여야 합니다")
    val content: String
) {

    /**
     * <h3>POST/COMMENT 신고 시 targetId 필수 검증</h3>
     * <p>POST와 COMMENT 타입의 신고는 targetId가 반드시 필요합니다.</p>
     *
     * @return boolean POST/COMMENT 타입일 때 targetId가 null이 아니면 true, 그 외 타입은 항상 true
     */
    @AssertTrue(message = "글, 댓글 신고는 신고대상이 필수입니다")
    fun isTargetIdRequiredForContentReport(): Boolean {
        if (reportType == ReportType.POST || reportType == ReportType.COMMENT) {
            return targetId != null
        }
        return true
    }

    /**
     * <h3>ERROR/IMPROVEMENT 신고 시 targetId null 검증</h3>
     * <p>ERROR와 IMPROVEMENT 타입의 신고는 targetId가 필요하지 않습니다.</p>
     *
     * @return boolean ERROR/IMPROVEMENT 타입일 때 targetId가 null이면 true, 그 외 타입은 항상 true
     */
    @AssertTrue(message = "에러, 개선 신고는 신고대상이 없어야 합니다")
    fun isTargetIdNotAllowedForSystemReport(): Boolean {
        if (reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) {
            return targetId == null
        }
        return true
    }
}
