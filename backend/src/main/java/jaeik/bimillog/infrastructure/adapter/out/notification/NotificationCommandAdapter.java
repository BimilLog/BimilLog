package jaeik.bimillog.infrastructure.adapter.out.notification;

import jaeik.bimillog.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.member.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 명령 어댑터</h2>
 * <p>알림 명령 작업을 담당하는 영속성 어댑터입니다.</p>
 * <p>알림 저장, 알림 일괄 업데이트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationCommandAdapter implements NotificationCommandPort {

    private final NotificationRepository notificationRepository;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>주어진 알림 ID 목록에 따라 알림을 삭제하거나 읽음 상태로 변경합니다.</p>
     * <p>삭제: Repository 메서드로 일괄 삭제</p>
     * <p>읽음 처리: 엔티티 조회 후 markAsRead() 호출하여 더티체킹 활용</p>
     *
     * @param userId           현재 로그인한 사용자 ID
     * @param updateCommand 업데이트할 알림 정보 명령 (삭제할 ID 목록, 읽음 처리할 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void batchUpdate(Long userId, NotificationUpdateVO updateCommand) {
        List<Long> deleteIds = updateCommand.deletedIds();
        List<Long> readIds = updateCommand.readIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndUsersId(deleteIds, userId);
        }

        if (readIds != null && !readIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllByIdInAndUsersId(readIds, userId);
            notifications.forEach(Notification::markAsRead);
        }
    }

    /**
     * <h3>알림 저장</h3>
     * <p>새로운 알림을 데이터베이스에 저장합니다.</p>
     *
     * @param member    알림을 받을 사용자 엔티티
     * @param type    알림 유형
     * @param content 알림 내용
     * @param url     알림 클릭 시 이동할 URL
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(Member member, NotificationType type, String content, String url) {
        notificationRepository.save(Notification.create(member, type, content, url));
    }

    /**
     * <h3>사용자의 모든 알림 삭제</h3>
     * <p>특정 사용자의 모든 알림을 데이터베이스에서 삭제합니다.</p>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 알림 데이터를 정리하는데 사용됩니다.</p>
     *
     * @param userId 알림을 삭제할 대상 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByUserId(Long userId) {
        notificationRepository.deleteAllByUsersId(userId);
    }
}