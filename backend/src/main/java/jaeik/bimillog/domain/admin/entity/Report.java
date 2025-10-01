package jaeik.bimillog.domain.admin.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>신고 엔티티</h2>
 * <p>사용자가 제출한 신고 및 건의사항 정보를 담는 엔티티입니다.</p>
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
    @JoinColumn(name = "member_id")
    private Member reporter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    private Long targetId;

    @Column(nullable = false)
    private String content;

    /**
     * <h3>신고 엔티티 생성을 위한 정적 팩토리 메서드</h3>
     * <p>AdminCommandService에서 신고 접수 시 호출되어 비즈니스 규칙을 준수하는 Report 엔티티를 생성합니다.</p>
     * <p>사용자나 관리자가 신고를 제출할 때 createReport 메서드에서 인스턴스를 생성합니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT)
     * @param targetId 신고 대상 ID (POST/COMMENT 신고 시 해당 ID, ERROR/IMPROVEMENT 시 null)
     * @param content 신고 내용 및 상세 설명
     * @param reporter 신고자 Member 엔티티 (익명 신고 시 null)
     * @return Report 비즈니스 규칙을 준수하는 새로운 신고 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Report createReport(ReportType reportType, Long targetId, String content, Member reporter) {
        return Report.builder()
                .reportType(reportType)
                .targetId(targetId)
                .content(content)
                .reporter(reporter)
                .build();
    }
}
