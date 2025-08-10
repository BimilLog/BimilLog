package jaeik.growfarm.repository.notification.update;

import jaeik.growfarm.repository.notification.NotificationBaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 업데이트 레포지터리 구현체</h2>
 * <p>
 * 알림 상태 변경 관련 기능을 구현
 * SRP: 알림 상태 변경 기능만 구현
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public class NotificationUpdateRepositoryImpl extends NotificationBaseRepository implements NotificationUpdateRepository {

    public NotificationUpdateRepositoryImpl(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public void markAsReadByIdInAndUserId(List<Long> ids, Long userId) {
        jpaQueryFactory
                .update(notification)
                .set(notification.isRead, true)
                .where(notification.id.in(ids)
                        .and(getUserNotificationCondition(userId)))
                .execute();
    }

}