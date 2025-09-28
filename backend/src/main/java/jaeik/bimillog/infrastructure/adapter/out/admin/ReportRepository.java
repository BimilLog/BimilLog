package jaeik.bimillog.infrastructure.adapter.out.admin;

import jaeik.bimillog.domain.admin.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h2>신고 저장소</h2>
 * <p>
 * Spring Data JPA를 사용하여 신고(`Report`) 엔티티의 기본적인 CRUD 작업을 제공하는 저장소 인터페이스
 * </p>
 * <p>
 * 복잡한 쿼리는 AdminQueryAdapter에서 QueryDSL을 사용하여 처리됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * <h3>특정 사용자의 모든 신고 삭제</h3>
     * <p>신고자 ID를 기준으로 해당 사용자가 작성한 모든 신고를 삭제합니다.</p>
     * <p>주로 사용자 탈퇴 시 해당 사용자의 모든 신고를 정리하는데 사용됩니다.</p>
     *
     * @param userId 삭제할 신고들의 신고자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByReporterId(Long userId);
}
