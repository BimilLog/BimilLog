package jaeik.bimillog.domain.admin.dto;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.member.entity.Member;
import lombok.*;

import java.time.Instant;

/**
 * <h2>신고 상세 DTO</h2>
 * <p>신고 조회 응답에 사용되는 데이터 전송 객체</p>
 * <p>신고 대상 작성자 정보까지 포함하여 관리자가 확인할 수 있도록 구성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailDTO {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private ReportType reportType;
    private Long targetId;
    private Long targetAuthorId;
    private String targetAuthorName;
    private String content;
    private Instant createdAt;

    /**
     * <h3>Report 엔티티와 신고 대상 작성자 정보로부터 ReportDetailDTO 생성</h3>
     *
     * @param report 신고 엔티티
     * @param targetAuthor 신고 대상 작성자 (게시글 또는 댓글 작성자)
     * @return ReportDetailDTO 변환된 DTO 객체
     */
    public static ReportDetailDTO from(Report report, Member targetAuthor) {
        return from(report,
                targetAuthor != null ? targetAuthor.getId() : null,
                targetAuthor != null ? targetAuthor.getMemberName() : null);
    }

    public static ReportDetailDTO from(Report report, Long targetAuthorId, String targetAuthorName) {
        Member reporter = report.getReporter();
        return ReportDetailDTO.builder()
                .id(report.getId())
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterName(reporter != null ? reporter.getMemberName() : "익명")
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .targetAuthorId(targetAuthorId)
                .targetAuthorName(targetAuthorName != null ? targetAuthorName : "익명")
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
