package jaeik.bimillog.infrastructure.adapter.admin.out.admin;

import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.infrastructure.adapter.admin.out.jpa.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>관리자 명령 어댑터</h2>
 * <p>관리자 도메인의 명령 작업을 위한 Secondary Adapter</p>
 * <p>AdminCommandPort 인터페이스를 구현하여 실제 데이터 저장소와의 연동을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AdminCommandAdapter implements AdminCommandPort {

    private final ReportRepository reportRepository;

    /**
     * {@inheritDoc}
     *
     * <p>ReportRepository를 사용하여 신고 엔티티를 데이터베이스에 저장합니다.</p>
     */
    @Override
    public Report save(Report report) {
        return reportRepository.save(report);
    }
}