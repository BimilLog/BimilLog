package jaeik.bimillog.infrastructure.adapter.out.notification.jpa;

import jaeik.bimillog.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>FCM 토큰 저장소</h2>
 * <p>FCM 토큰 관련 데이터베이스 작업을 수행하는 JPA 저장소</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 토큰을 삭제할 사용자의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUser_Id(Long userId);

    /**
     * <h3>사용자 ID와 토큰 ID로 특정 FCM 토큰 삭제</h3>
     * <p>특정 사용자의 특정 FCM 토큰을 삭제합니다.</p>
     * <p>다중 기기 로그인 환경에서 특정 기기의 토큰만 선택적으로 삭제</p>
     *
     * @param userId 토큰을 삭제할 사용자의 ID
     * @param fcmTokenId 삭제할 FCM 토큰의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUser_IdAndId(Long userId, Long fcmTokenId);

}
