package jaeik.bimillog.domain.admin.entity;

import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.common.entity.BaseEntity;
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

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String content;

    /**
     * <h3>ReportVO로부터 Report 엔티티 생성</h3>
     * <p>ReportVO와 사용자 정보를 사용하여 새로운 Report 엔티티를 생성합니다.</p>
     *
     * @param reportVO 신고 정보 값 객체
     * @param user     신고한 사용자 엔티티
     * @return Report 생성된 신고 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Report createReport(ReportVO reportVO, User user) {
        return Report.builder()
                .content(reportVO.content())
                .reportType(reportVO.reportType())
                .targetId(reportVO.targetId())
                .reporter(user)
                .build();
    }
}
