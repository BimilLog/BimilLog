package jaeik.growfarm.repository.notification.delete;

import jaeik.growfarm.repository.notification.NotificationBaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 삭제 레포지터리 구현체</h2>
 * <p>
 * 알림 삭제 관련 기능을 구현
 * SRP: 알림 삭제 기능만 구현
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public class NotificationDeleteRepositoryImpl extends NotificationBaseRepository implements NotificationDeleteRepository {

    public NotificationDeleteRepositoryImpl(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    @Override
    public void deleteByIdInAndUserId(List<Long> ids, Long userId) {
        jpaQueryFactory
                .delete(notification)
                .where(notification.id.in(ids)
                        .and(getUserNotificationCondition(userId)))
                .execute();
    }

}