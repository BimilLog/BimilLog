package jaeik.growfarm.infrastructure.adapter.notification.out.persistence.notification;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.growfarm.domain.notification.entity.QNotification;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.NotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
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
     * <p>현재 로그인한 사용자의 알림 목록을 최신순으로 조회합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 알림 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationList(CustomUserDetails userDetails) {

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
                .where(notification.users.id.eq(userDetails.getUserId()))
                .orderBy(notification.createdAt.desc())
                .fetch();
    }
}
