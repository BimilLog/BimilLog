package jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification;

import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * <h3>알림 타입별 유효한 FCM 토큰 조회</h3>
     * <p>
     * 특정 알림 타입이 활성화된 사용자의 FCM 토큰만 조회합니다.
     * 알림 타입에 따라 해당하는 설정 필드를 동적으로 확인합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 타입 (PAPER, COMMENT, POST_FEATURED)
     * @return 유효한 FCM 토큰 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("""
        SELECT f FROM FcmToken f
        JOIN f.user u
        JOIN u.setting s
        WHERE u.id = :userId
        AND (
            (:notificationType = 'PAPER' AND s.messageNotification = true) OR
            (:notificationType = 'COMMENT' AND s.commentNotification = true) OR
            (:notificationType = 'POST_FEATURED' AND s.postFeaturedNotification = true)
        )
        """)
    List<FcmToken> findValidFcmTokensByNotificationType(
            @Param("userId") Long userId, 
            @Param("notificationType") NotificationType notificationType);


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
