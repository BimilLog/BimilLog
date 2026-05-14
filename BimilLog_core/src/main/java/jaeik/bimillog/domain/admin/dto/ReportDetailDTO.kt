package jaeik.bimillog.domain.admin.dto

import jaeik.bimillog.domain.admin.entity.Report
import jaeik.bimillog.domain.admin.entity.ReportType
import java.time.Instant

/**
 * <h2>신고 상세 DTO</h2>
 * <p>신고 조회 응답에 사용되는 데이터 전송 객체</p>
 * <p>신고 대상 작성자 정보까지 포함하여 관리자가 확인할 수 있도록 구성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
data class ReportDetailDTO(
    val id: Long,
    val reporterId: Long?,
    val reporterName: String,
    val reportType: ReportType,
    val targetId: Long?,
    val targetAuthorId: Long?,
    val targetAuthorName: String,
    val content: String,
    val createdAt: Instant
) {
    companion object {

        /**
         * <h3>Report 엔티티와 신고 대상 작성자 정보로부터 ReportDetailDTO 생성</h3>
         *
         * @param report 신고 엔티티
         * @return ReportDetailDTO 변환된 DTO 객체
         */
        @JvmStatic
        fun from(report: Report, targetAuthorId: Long?, targetAuthorName: String?) : ReportDetailDTO {
            val reporter = report.reporter
            return ReportDetailDTO(
                id = report.id,
                reporterId = reporter?.id,
                reporterName = reporter?.memberName ?: "익명",
                reportType = report.reportType,
                targetId = report.targetId,
                targetAuthorId = targetAuthorId,
                targetAuthorName = targetAuthorName?: "익명",
                content = report.content,
                createdAt = report.createdAt
            )
        }
    }
}
