package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.notification.entity.QNotification;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.dto.notification.NotificationDTO;
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
public class NotificationReadRepository {

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
    public NotificationReadRepository(com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory, JPAQueryFactory jpaQueryFactory1) {
        this.jpaQueryFactory = jpaQueryFactory1;
    }

    /**
     * <h3>사용자 ID로 최신순 알림 목록 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 알림 목록을 최신 생성일 기준으로 정렬하여 조회합니다.</p>
     *
     * @param userId 알림을 조회할 사용자의 ID
     * @return 알림 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<NotificationDTO> findNotificationsByUserIdOrderByLatest(Long userId) {
        QUser user = QUser.user;

        return jpaQueryFactory
                .select(Projections.constructor(NotificationDTO.class,
                        notification.id,
                        notification.notificationType,
                        notification.content,
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