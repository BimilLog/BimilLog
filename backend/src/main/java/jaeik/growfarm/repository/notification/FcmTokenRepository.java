package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>FCM 토큰 Repository</h2>
 * <p>FCM 토큰 관련 데이터베이스 작업을 수행하는 Repository</p>
 * @since 1.0.0
 * @author Jaeik
 */
@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    /**
     * <h3>사용자 ID로 유효한 FCM 토큰 조회</h3>
     * <p>
     * 메시지 알림이 활성화된 사용자의 FCM 토큰만 조회합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 유효한 FCM 토큰 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @Query("""
        SELECT f FROM FcmToken f
        JOIN f.users u
        JOIN u.setting s
        WHERE u.id = :userId
        AND s.messageNotification = true
        """)
    List<FcmToken> findValidFcmTokensByUserId(@Param("userId") Long userId);

    void deleteByUsers_Id(Long userId);

}
