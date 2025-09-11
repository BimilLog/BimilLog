package jaeik.bimillog.domain.admin.application.port.out;

import jaeik.bimillog.domain.admin.application.service.AdminCommandService;
import jaeik.bimillog.domain.admin.entity.Report;

/**
 * <h2>관리자 명령 포트</h2>
 * <p>관리자의 명령을 담당하는 포트입니다.</p>
 * <p>신고 엔티티 저장</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandPort {

    /**
     * <h3>신고 엔티티 저장</h3>
     * <p>신고 엔티티를 데이터베이스에 저장합니다.</p>
     * <p>JPA ID 자동 생성, @CreatedDate 자동 설정</p>
     * <p>{@link AdminCommandService}에서 신고 데이터 저장 시 호출합니다.</p>
     *
     * @param report 저장할 신고 엔티티 (ID는 null, 기본 정보만 설정된 상태)
     * @return Report 저장 완료된 신고 엔티티 (ID와 생성일시 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    Report save(Report report);
}