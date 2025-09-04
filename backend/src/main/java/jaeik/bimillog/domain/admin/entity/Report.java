package jaeik.bimillog.domain.admin.entity;

import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.common.entity.BaseEntity;
import jaeik.bimillog.infrastructure.exception.AdminCustomException;
import jaeik.bimillog.infrastructure.exception.AdminErrorCode;
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
     * <h3>Report 엔티티 생성</h3>
     * <p>개별 파라미터로부터 새로운 Report 엔티티를 생성합니다.</p>
     * <p>targetId와 reportType 간의 비즈니스 규칙을 검증합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @param content    신고 내용
     * @param reporter   신고한 사용자 엔티티
     * @return Report 생성된 신고 엔티티
     * @throws AdminCustomException targetId 관련 비즈니스 규칙 위반 시
     * @author Jaeik
     * @since 2.0.0
     */
    public static Report createReport(ReportType reportType, Long targetId, String content, User reporter) {
        validateTargetIdRules(reportType, targetId);
        
        return Report.builder()
                .reportType(reportType)
                .targetId(targetId)
                .content(content)
                .reporter(reporter)
                .build();
    }
    
    /**
     * <h3>targetId 비즈니스 규칙 검증</h3>
     * <p>reportType과 targetId 조합의 비즈니스 규칙을 검증합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @throws AdminCustomException 비즈니스 규칙 위반 시
     * @author Jaeik
     * @since 2.0.0
     */
    private static void validateTargetIdRules(ReportType reportType, Long targetId) {
        // POST, COMMENT 신고는 targetId 필수
        if ((reportType == ReportType.POST || reportType == ReportType.COMMENT) 
                && targetId == null) {
            throw new AdminCustomException(AdminErrorCode.REPORT_TARGET_REQUIRED);
        }
        
        // ERROR, IMPROVEMENT는 targetId가 없어야 함
        if ((reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) 
                && targetId != null) {
            throw new AdminCustomException(AdminErrorCode.REPORT_TARGET_NOT_ALLOWED);
        }
    }
}
