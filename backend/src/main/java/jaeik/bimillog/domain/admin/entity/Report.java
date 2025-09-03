package jaeik.bimillog.domain.admin.entity;

import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.common.entity.BaseEntity;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>신고 엔티티</h2>
 * <p>
 * 사용자가 신고한 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 신고자, 신고 유형, 대상 ID, 신고 내용을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "report")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User reporter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    private Long targetId;

    @Column(nullable = false)
    private String content;

    /**
     * <h3>ReportVO로부터 Report 엔티티 생성</h3>
     * <p>ReportVO와 사용자 정보를 사용하여 새로운 Report 엔티티를 생성합니다.</p>
     * <p>도메인 규칙에 따른 검증을 수행하여 유효한 신고만 생성됩니다.</p>
     *
     * @param reportVO 신고 정보 값 객체
     * @param user     신고한 사용자 엔티티
     * @return Report 생성된 신고 엔티티
     * @throws CustomException 도메인 규칙 위반 시
     * @author Jaeik
     * @since 2.0.0
     */
    public static Report createReport(ReportVO reportVO, User user) {
        validateReportVO(reportVO);
        
        return Report.builder()
                .content(reportVO.content())
                .reportType(reportVO.reportType())
                .targetId(reportVO.targetId())
                .reporter(user)
                .build();
    }
    
    /**
     * <h3>ReportVO 도메인 검증</h3>
     * <p>Report 엔티티 생성 전 도메인 규칙을 검증합니다.</p>
     * <p>이는 마지막 방어선 역할을 하며, 도메인 무결성을 보장합니다.</p>
     *
     * @param reportVO 검증할 신고 정보 값 객체
     * @throws CustomException 도메인 규칙 위반 시
     * @author Jaeik
     * @since 2.0.0
     */
    private static void validateReportVO(ReportVO reportVO) {
        if (reportVO == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (reportVO.reportType() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (reportVO.content() == null || reportVO.content().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (reportVO.content().length() > 500) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        // POST, COMMENT 신고는 targetId 필수
        if ((reportVO.reportType() == ReportType.POST || reportVO.reportType() == ReportType.COMMENT) 
                && reportVO.targetId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }
        
        // ERROR, IMPROVEMENT는 targetId가 없어야 함
        if ((reportVO.reportType() == ReportType.ERROR || reportVO.reportType() == ReportType.IMPROVEMENT) 
                && reportVO.targetId() != null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }
    }
}
