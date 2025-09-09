package jaeik.bimillog.domain.admin.application.port.out;

import jaeik.bimillog.domain.admin.entity.Report;

/**
 * <h2>AdminCommandPort</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 명령 작업을 위한 Secondary Port 인터페이스입니다.
 * </p>
 * <p>
 * 도메인 계층이 인프라스트럭처 계층에 직접 의존하지 않도록 하는 의존성 역전의 핵심 역할을 합니다.
 * </p>
 * <p>
 * AdminCommandService에서 신고 데이터 저장이 필요할 때 이 인터페이스를 호출하고,
 * AdminCommandAdapter에서 실제 JPA Repository를 통한 데이터베이스 연동을 구현합니다.
 * </p>
 * <p>
 * 도메인의 순수성을 유지하면서도 필요한 영속성 작업을 추상화하여 테스트 용이성과 유연성을 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandPort {

    /**
     * <h3>신고 엔티티 영속화</h3>
     * <p>AdminCommandService에서 생성된 신고 엔티티를 데이터베이스에 저장합니다.</p>
     * <p>사용자나 관리자가 신고를 제출할 때 createReport 메서드에서 호출되어 Report 엔티티를 영속화합니다.</p>
     * <p>AdminCommandAdapter를 통해 ReportRepository의 save 메서드로 실제 저장 작업이 수행됩니다.</p>
     * <p>생성된 ID와 타임스탬프 등 영속화 과정에서 생성된 정보를 포함한 완전한 엔티티를 반환합니다.</p>
     *
     * @param report 저장할 신고 엔티티 (ID는 null, 기본 정보만 설정된 상태)
     * @return Report 저장 완료된 신고 엔티티 (ID와 생성일시 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    Report save(Report report);
}