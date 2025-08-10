package jaeik.growfarm.repository.notification.read;

import com.querydsl.core.types.Projections;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.entity.notification.QNotification;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.notification.NotificationBaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 읽기 레포지터리 구현체</h2>
 * <p>
 * 알림 조회 관련 기능을 구현
 * SRP: 알림 조회 기능만 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public class NotificationReadRepositoryImpl extends NotificationBaseRepository implements NotificationReadRepository {

    public NotificationReadRepositoryImpl(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public List<NotificationDTO> findNotificationsByUserIdOrderByLatest(Long userId) {
        QUsers user = QUsers.users;

        return jpaQueryFactory
                .select(Projections.constructor(NotificationDTO.class,
                        notification.id,
                        notification.notificationType,
                        notification.data,
                        notification.url,
                        notification.isRead,
                        notification.createdAt))
                .from(notification)
                .leftJoin(notification.users, user)
                .where(getUserNotificationCondition(userId))
                .orderBy(notification.createdAt.desc())
                .fetch();
    }

}