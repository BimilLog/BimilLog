package jaeik.growfarm.infrastructure.adapter.notification.out.persistence.notification;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.growfarm.domain.notification.entity.Notification;
import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.domain.notification.entity.QNotification;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.notification.entity.NotificationUpdateCommand;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 명령 영속성 어댑터</h2>
 * <p>알림 상태 변경 관련 데이터베이스 작업을 처리합니다.</p>
 * <p>NotificationCommandPort 인터페이스를 구현하여 알림 저장 및 일괄 업데이트 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationCommandAdapter implements NotificationCommandPort {

    private final NotificationRepository notificationRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final QNotification notification = QNotification.notification;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>주어진 알림 ID 목록에 따라 알림을 삭제하거나 읽음 상태로 변경합니다.</p>
     *
     * @param userDetails           현재 로그인한 사용자 정보
     * @param updateCommand 업데이트할 알림 정보 명령 (삭제할 ID 목록, 읽음 처리할 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void batchUpdate(CustomUserDetails userDetails, NotificationUpdateCommand updateCommand) {
        Long userId = userDetails.getUserId();
        List<Long> deleteIds = updateCommand.deletedIds();
        List<Long> readIds = updateCommand.readIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            jpaQueryFactory
                    .delete(notification)
                    .where(notification.id.in(deleteIds)
                            .and(notification.users.id.eq(userId)))
                    .execute();
        }

        if (readIds != null && !readIds.isEmpty()) {
            jpaQueryFactory
                    .update(notification)
                    .set(notification.isRead, true)
                    .where(notification.id.in(readIds)
                            .and(notification.users.id.eq(userId)))
                    .execute();
        }
    }

    /**
     * <h3>알림 저장</h3>
     * <p>새로운 알림을 데이터베이스에 저장합니다.</p>
     *
     * @param user    알림을 받을 사용자 엔티티
     * @param type    알림 유형
     * @param content 알림 내용
     * @param url     알림 클릭 시 이동할 URL
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(User user, NotificationType type, String content, String url) {
        notificationRepository.save(Notification.create(user, type, content, url));
    }
}