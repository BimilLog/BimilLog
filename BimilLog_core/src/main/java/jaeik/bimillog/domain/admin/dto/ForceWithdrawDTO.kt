package jaeik.bimillog.domain.admin.dto

import jaeik.bimillog.domain.admin.entity.ReportType
import jakarta.validation.constraints.NotNull

data class ForceWithdrawDTO(

    @field:NotNull(message = "신고 유형은 필수입니다")
    val reportType: ReportType,

    @field:NotNull(message = "신고 대상 ID는 필수입니다")
    val targetId: Long
)