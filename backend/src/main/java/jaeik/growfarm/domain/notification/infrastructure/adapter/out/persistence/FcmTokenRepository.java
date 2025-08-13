package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>FCM 토큰 Repository</h2>
 * <p>FCM 토큰 관련 데이터베이스 작업을 수행하는 Repository</p>
 * @since 2.0.0
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
     * @since 2.0.0
     */
    @Query("""
        SELECT f FROM FcmToken f
        JOIN f.user u
        JOIN u.setting s
        WHERE u.id = :userId
        AND s.messageNotification = true
        """)
    List<FcmToken> findValidFcmTokensByUserId(@Param("userId") Long userId);

    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 토큰을 삭제할 사용자의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUser_Id(Long userId);

}
