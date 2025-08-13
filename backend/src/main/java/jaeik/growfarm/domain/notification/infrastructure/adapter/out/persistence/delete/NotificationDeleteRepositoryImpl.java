package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.delete;

import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.NotificationBaseRepository;
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
 * @version 2.0.0
 */
@Repository
public class NotificationDeleteRepositoryImpl extends NotificationBaseRepository implements NotificationDeleteRepository {

    /**
     * <h3>생성자</h3>
     * <p>JPAQueryFactory를 주입받아 초기화합니다.</p>
     *
     * @param jpaQueryFactory JPAQueryFactory 주입
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationDeleteRepositoryImpl(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory) {
        super(jpaQueryFactory);
    }

    /**
     * <h3>알림 ID 목록과 사용자 ID로 알림 삭제</h3>
     * <p>주어진 알림 ID 목록과 사용자 ID에 해당하는 알림을 삭제합니다.</p>
     *
     * @param ids 삭제할 알림 ID 목록
     * @param userId 알림을 소유한 사용자의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByIdInAndUserId(List<Long> ids, Long userId) {
        jpaQueryFactory
                .delete(notification)
                .where(notification.id.in(ids)
                        .and(getUserNotificationCondition(userId)))
                .execute();
    }

}