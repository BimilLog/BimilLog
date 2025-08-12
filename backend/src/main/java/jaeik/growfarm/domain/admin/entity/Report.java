package jaeik.growfarm.domain.admin.entity;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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

    public static Report DtoToReport(ReportDTO reportDTO, User user) {
        return Report.builder()
                .content(reportDTO.getContent())
                .reportType(reportDTO.getReportType())
                .targetId(reportDTO.getTargetId())
                .reporter(user)
                .build();
    }
}
