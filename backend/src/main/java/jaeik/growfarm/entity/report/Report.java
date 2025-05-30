package jaeik.growfarm.entity.report;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 신고 엔티티
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private Users users;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    private Long targetId;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false) // 신고 내용 500자 허용
    private String content;

    public static Report DtoToReport(ReportDTO reportDTO, Users user) {
        return Report.builder()
                .reportType(reportDTO.getReportType())
                .users(user)
                .targetId(reportDTO.getTargetId())
                .content(reportDTO.getContent())
                .build();
    }
}
