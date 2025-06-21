package jaeik.growfarm.repository.notification;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.entity.notification.QNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 커스텀 저장소 구현 클래스</h2>
 * <p>
 * 알림 관련 데이터베이스 작업을 수행하는 커스텀 저장소 구현 클래스입니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationCustomRepositoryImpl implements NotificationCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>사용자 ID로 알림 목록 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 알림 목록을 최신순으로 조회합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return List<NotificationDTO> 알림 DTO 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public List<NotificationDTO> findNotificationsByUserIdOrderByLatest(Long userId) {
        QNotification notification = QNotification.notification;

        return jpaQueryFactory
                .select(Projections.fields(NotificationDTO.class,
                        notification.id,
                        notification.data,
                        notification.url,
                        notification.notificationType.as("type"),
                        notification.isRead,
                        notification.createdAt))
                .from(notification)
                .where(notification.users.id.eq(userId))
                .orderBy(notification.createdAt.desc())
                .limit(20)
                .fetch();
    }

    /**
     * <h3>알림 삭제</h3>
     * <p>
     * 알림 ID 목록과 사용자 ID를 기준으로 알림을 삭제합니다.
     * </p>
     *
     * @param ids    삭제할 알림 ID 목록
     * @param userId 사용자 ID
     * @return int 삭제된 알림의 수
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public int deleteByIdInAndUserId(List<Long> ids, Long userId) {
        QNotification notification = QNotification.notification;

        return (int) jpaQueryFactory
                .delete(notification)
                .where(notification.id.in(ids)
                        .and(notification.users.id.eq(userId)))
                .execute();
    }

    /**
     * <h3>알림 읽음 처리</h3>
     * <p>
     * 알림 ID 목록과 사용자 ID를 기준으로 알림을 읽음 처리합니다.
     * </p>
     *
     * @param ids    읽음 처리할 알림 ID 목록
     * @param userId 사용자 ID
     * @return int 읽음 처리된 알림의 수
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public int markAsReadByIdInAndUserId(List<Long> ids, Long userId) {
        QNotification notification = QNotification.notification;

        return (int) jpaQueryFactory
                .update(notification)
                .set(notification.isRead, true)
                .where(notification.id.in(ids)
                        .and(notification.users.id.eq(userId)))
                .execute();
    }
}
