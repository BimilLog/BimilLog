package jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.bimillog.domain.notification.entity.NotificationInfo;
import jaeik.bimillog.domain.notification.entity.QNotification;
import jaeik.bimillog.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>알림 쿼리 영속성 어댑터</h2>
 * <p>알림 조회와 관련된 데이터베이스 작업을 처리합니다.</p>
 * <p>NotificationQueryPort 인터페이스를 구현하여 알림 조회 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationQueryAdapter implements NotificationQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final QNotification notification = QNotification.notification;
    private final QUser user = QUser.user;

    /**
     * <h3>알림 목록 조회</h3>
     * <p>지정된 사용자의 알림 목록을 최신순으로 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return 알림 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationInfo> getNotificationList(Long userId) {

        return jpaQueryFactory
                .select(Projections.constructor(NotificationInfo.class,
                        notification.id,
                        notification.content,
                        notification.url,
                        notification.notificationType,
                        notification.isRead,
                        notification.createdAt))
                .from(notification)
                .innerJoin(notification.users, user)
                .where(notification.users.id.eq(userId))
                .orderBy(notification.createdAt.desc())
                .fetch();
    }
}
