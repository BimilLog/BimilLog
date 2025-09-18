package jaeik.bimillog.infrastructure.adapter.out.admin;

import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.application.service.AdminCommandService;
import jaeik.bimillog.domain.admin.entity.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>관리자 명령 어댑터</h2>
 * <p>관리자 도메인의 명령 작업을 담당하는 어댑터입니다.</p>
 * <p>신고 데이터 영속화</p>
 * <p>JPA Repository를 통한 데이터베이스 연동</p>
 * <p>AdminCommandPort 구현체</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AdminCommandAdapter implements AdminCommandPort {

    private final ReportRepository reportRepository;

    /**
     * <h3>신고 엔티티 데이터베이스 저장</h3>
     * <p>신고 엔티티를 MySQL 데이터베이스에 영속화합니다.</p>
     * <p>JPA @GeneratedValue로 ID 자동 생성, @CreatedDate로 생성일시 자동 설정</p>
     * <p>{@link AdminCommandService}에서 신고 데이터 저장 시 호출됩니다.</p>
     *
     * @param report 저장할 신고 엔티티 (ID null, 기본 정보 설정됨)
     * @return Report 저장 완료된 신고 엔티티 (ID와 생성일시 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Report save(Report report) {
        return reportRepository.save(report);
    }
}