package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.user.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/*
 * 알림 Repository
 * 알림 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUsers(Users users, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM notification WHERE user_id = :userId")
    void deleteNotificationsByUserId(@Param("userId") Long userId);
}
