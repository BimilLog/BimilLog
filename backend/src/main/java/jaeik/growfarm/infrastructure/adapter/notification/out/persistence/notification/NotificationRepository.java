package jaeik.growfarm.infrastructure.adapter.notification.out.persistence.notification;

import jaeik.growfarm.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>알림 기본 레포지토리</h2>
 * <p>
 * 알림 엔티티의 기본 CRUD 기능을 제공하는 레포지토리
 * SRP: 기본 엔티티 영속성 관리만 담당
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
