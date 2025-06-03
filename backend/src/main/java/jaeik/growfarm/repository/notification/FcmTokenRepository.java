package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * FCM 토큰 Repository
 * FCM 토큰 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUsers(Users user);

    void deleteByUserId(Long userId);

}
