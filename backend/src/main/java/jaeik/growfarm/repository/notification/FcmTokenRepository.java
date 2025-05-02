package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.DeviceType;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByUsersIdAndDeviceType(Long userId, DeviceType deviceType);

    List<FcmToken> findByUsers(Users user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM fcm_token WHERE user_id = :userId")
    void deleteFcmTokenByUserId(@Param("userId") Long userId);
}
