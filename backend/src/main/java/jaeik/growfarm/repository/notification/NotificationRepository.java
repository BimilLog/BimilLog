package jaeik.growfarm.repository.notification;

import jaeik.growfarm.entity.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>알림 레포지토리</h2>
 * <p>알림 데이터를 관리하는 레포지토리 인터페이스</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationCustomRepository {
}
