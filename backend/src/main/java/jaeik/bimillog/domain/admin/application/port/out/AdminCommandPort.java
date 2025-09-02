package jaeik.bimillog.domain.admin.application.port.out;

import jaeik.bimillog.domain.admin.entity.Report;

/**
 * <h2>관리자 명령 포트</h2>
 * <p>관리자 도메인의 명령 작업을 위한 외부 저장소 인터페이스</p>
 * <p>헥사고날 아키텍처에서 도메인이 인프라스트럭처에 의존하지 않도록 하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandPort {

    /**
     * <h3>신고 저장</h3>
     * <p>신고 엔티티를 저장소에 저장합니다.</p>
     *
     * @param report 저장할 신고 엔티티
     * @return Report 저장된 신고 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Report save(Report report);
}