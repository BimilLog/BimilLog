package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.user.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUsers(Users users, Pageable pageable);
}
