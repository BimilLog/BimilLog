package jaeik.bimillog.infrastructure.adapter.admin.out.admin;

import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.infrastructure.adapter.admin.out.jpa.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>AdminCommandAdapter</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 명령 작업을 위한 Secondary Adapter 구현체입니다.
 * </p>
 * <p>
 * AdminCommandPort 인터페이스를 구현하여 도메인 계층과 인프라스트럭처 계층 사이의 다리 역할을 합니다.
 * </p>
 * <p>
 * AdminCommandService에서 신고 데이터 저장 요청을 받아 ReportRepository를 통해 실제 데이터베이스 작업을 수행합니다.
 * </p>
 * <p>
 * JPA Repository의 추상화를 통해 데이터베이스 기술 변경에 유연하게 대응할 수 있도록 설계되었습니다.
 * </p>
 * <p>
 * 도메인의 순수성을 유지하면서 필요한 영속성 기능을 제공하는 Infrastructure Layer의 핵심 구성 요소입니다.
 * </p>
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
     * <p>AdminCommandPort 인터페이스의 save 메서드를 구현하여 신고 엔티티를 MySQL 데이터베이스에 영속화합니다.</p>
     * <p>AdminCommandService의 createReport 메서드에서 Report 엔티티 저장이 필요할 때 호출됩니다.</p>
     * <p>Spring Data JPA의 ReportRepository.save() 메서드를 사용하여 실제 INSERT 쿼리를 실행합니다.</p>
     * <p>JPA의 @GeneratedValue 전략에 따라 ID가 자동 생성되고 @CreatedDate로 생성일시가 자동 설정됩니다.</p>
     * <p>트랜잭션 내에서 실행되어 데이터 일관성을 보장하며, 영속화 완료된 엔티티를 반환합니다.</p>
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