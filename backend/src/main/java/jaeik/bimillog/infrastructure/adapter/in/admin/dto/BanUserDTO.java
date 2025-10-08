package jaeik.bimillog.infrastructure.adapter.in.admin.dto;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * <h2>사용자 제재 DTO</h2>
 * <p>관리자가 사용자를 제재할 때 사용하는 데이터 전송 객체</p>
 * <p>신고 유형과 대상 ID만 필수로 받아 간결하게 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanUserDTO {

    @NotNull(message = "신고 유형은 필수입니다")
    private ReportType reportType;

    @NotNull(message = "신고 대상 ID는 필수입니다")
    private Long targetId;
}
