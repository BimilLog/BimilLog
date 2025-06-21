package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<FcmToken> findByUsers(Users user);

    void deleteByUsers_Id(Long userId);

}
