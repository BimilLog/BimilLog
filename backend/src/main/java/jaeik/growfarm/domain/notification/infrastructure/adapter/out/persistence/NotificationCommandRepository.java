package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.notification.entity.QNotification;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 명령 레포지터리 구현체</h2>
 * <p>

 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public class NotificationCommandRepository {

    protected final JPAQueryFactory jpaQueryFactory;
    protected final QNotification notification = QNotification.notification;

    /**
     * <h3>사용자별 알림 기본 조건</h3>
     * <p>공통으로 사용되는 사용자별 알림 필터링 조건을 제공합니다.</p>
     *
     * @param userId 사용자 ID
     * @return 공통 조건
     */
    protected com.querydsl.core.types.dsl.BooleanExpression getUserNotificationCondition(Long userId) {
        return notification.users.id.eq(userId);
    }

    /**
     * <h3>생성자</h3>
     * <p>JPAQueryFactory를 주입받아 초기화합니다.</p>
     *
     * @param jpaQueryFactory JPAQueryFactory 주입
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationCommandRepository(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory, JPAQueryFactory jpaQueryFactory1) {
        this.jpaQueryFactory = jpaQueryFactory1;
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
    public void deleteByIdInAndUserId(List<Long> ids, Long userId) {
        jpaQueryFactory
                .delete(notification)
                .where(notification.id.in(ids)
                        .and(getUserNotificationCondition(userId)))
                .execute();
    }

    /**
     * <h3>알림 ID 목록과 사용자 ID로 알림 읽음 처리</h3>
     * <p>주어진 알림 ID 목록과 사용자 ID에 해당하는 알림을 읽음 상태로 변경합니다.</p>
     *
     * @param ids 읽음 처리할 알림 ID 목록
     * @param userId 알림을 소유한 사용자의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void markAsReadByIdInAndUserId(List<Long> ids, Long userId) {
        jpaQueryFactory
                .update(notification)
                .set(notification.isRead, true)
                .where(notification.id.in(ids)
                        .and(getUserNotificationCondition(userId)))
                .execute();
    }

}