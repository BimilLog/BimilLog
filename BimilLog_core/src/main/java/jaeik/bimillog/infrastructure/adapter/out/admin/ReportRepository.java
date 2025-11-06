package jaeik.bimillog.infrastructure.adapter.out.admin;

import jaeik.bimillog.domain.admin.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * <h2>신고 저장소</h2>
 * <p>신고 엔티티 스프링 데이터 저장소</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * <h3>특정 사용자가 작성한 신고 조회</h3>
     * <p>회원 탈퇴 시 reporter 연관을 제거하기 위해 사용됩니다.</p>
     *
     * @param memberId 신고를 조회할 사용자 ID
     * @return List<Report> 해당 사용자가 작성한 신고 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Report> findAllByReporterId(Long memberId);
}
