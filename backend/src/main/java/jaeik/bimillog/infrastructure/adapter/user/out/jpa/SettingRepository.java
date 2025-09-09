package jaeik.bimillog.infrastructure.adapter.user.out.jpa;

import jaeik.bimillog.domain.user.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
