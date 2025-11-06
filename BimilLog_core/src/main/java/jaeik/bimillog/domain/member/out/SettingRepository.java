package jaeik.bimillog.domain.member.out;

import jaeik.bimillog.domain.member.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * <h2>설정 레포지토리 인터페이스</h2>
 * <p>
 * 사용자 설정 관련 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

    /**
     * <h3>memberId로 Setting 삭제</h3>
     * <p>Member의 settingId를 서브쿼리로 찾아서 Setting을 삭제합니다.</p>
     * <p>회원 탈퇴 시 Member 삭제 전에 호출되어 Setting 레코드를 정리합니다.</p>
     *
     * @param memberId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM setting WHERE setting_id = " +
                   "(SELECT setting_id FROM member WHERE member_id = :memberId)",
           nativeQuery = true)
    void deleteByMemberId(@Param("memberId") Long memberId);

}
