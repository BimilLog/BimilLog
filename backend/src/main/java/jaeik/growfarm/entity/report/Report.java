package jaeik.growfarm.entity.report;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    private Long targetId;

    @NotNull
    @Column(nullable = false)
    private String content;
}
