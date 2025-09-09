package jaeik.bimillog.domain.admin.entity;

import jaeik.bimillog.global.entity.BaseEntity;
import jaeik.bimillog.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>Report</h2>
 * <p>
 * Admin 도메인의 핵심 엔티티로서 사용자가 제출한 신고 및 건의사항 정보를 표현하는 도메인 객체입니다.
 * </p>
 * <p>
 * DDD의 Entity 패턴을 따라 생명주기를 가지며, 신고 접수부터 관리자 처리까지의 전체 과정을 담당합니다.
 * </p>
 * <p>
 * 사용자가 부적절한 게시글, 댓글을 신고하거나 시스템 오류, 개선사항을 제안할 때 생성되는 도메인 객체입니다.
 * </p>
 * <p>
 * 신고자(User), 신고 유형(ReportType), 신고 대상 식별자, 신고 내용을 속성으로 가지며,
 * 관리자가 제재나 강제 탈퇴 결정을 내릴 때 중요한 판단 근거가 됩니다.
 * </p>
 * <p>
 * BaseEntity를 상속하여 생성일시, 수정일시 등 공통 속성을 포함하고,
 * 정적 팩토리 메서드를 통해 비즈니스 규칙을 준수하는 인스턴스 생성을 보장합니다.
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
     * <h3>신고 엔티티 생성을 위한 정적 팩토리 메서드</h3>
     * <p>AdminCommandService에서 신고 접수 시 호출되어 비즈니스 규칙을 준수하는 Report 엔티티를 생성합니다.</p>
     * <p>사용자나 관리자가 신고를 제출할 때 createReport 메서드에서 이 팩토리를 사용하여 안전한 인스턴스를 생성합니다.</p>
     * <p>생성자를 직접 사용하는 대신 정적 팩토리 메서드를 통해 도메인 규칙을 강제하고 의미를 명확히 합니다.</p>
     * <p>신고 유형과 대상 ID의 조합이 유효한지 검증은 상위 계층에서 수행하고, 여기서는 객체 생성에만 집중합니다.</p>
     * <p>Builder 패턴을 활용하여 불변성을 유지하면서도 가독성 있는 객체 생성을 지원합니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT)
     * @param targetId 신고 대상 ID (POST/COMMENT 신고 시 해당 ID, ERROR/IMPROVEMENT 시 null)
     * @param content 신고 내용 및 상세 설명
     * @param reporter 신고자 User 엔티티 (익명 신고 시 null)
     * @return Report 비즈니스 규칙을 준수하는 새로운 신고 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Report createReport(ReportType reportType, Long targetId, String content, User reporter) {

        return Report.builder()
                .reportType(reportType)
                .targetId(targetId)
                .content(content)
                .reporter(reporter)
                .build();
    }
}
