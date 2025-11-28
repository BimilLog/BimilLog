package jaeik.bimillog.domain.notification.out;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
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
public class NotificationCommandRepository {

    private final NotificationRepository notificationRepository;

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>주어진 알림 ID 목록에 따라 알림을 삭제하거나 읽음 상태로 변경합니다.</p>
     * <p>삭제: Repository 메서드로 일괄 삭제</p>
     * <p>읽음 처리: 엔티티 조회 후 markAsRead() 호출하여 더티체킹 활용</p>
     *
     * @param memberId           현재 로그인한 사용자 ID
     * @param updateCommand 업데이트할 알림 정보 명령 (삭제할 ID 목록, 읽음 처리할 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    public void batchUpdate(Long memberId, NotificationUpdateVO updateCommand) {
        List<Long> deleteIds = updateCommand.deletedIds();
        List<Long> readIds = updateCommand.readIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInAndMemberId(deleteIds, memberId);
        }

        if (readIds != null && !readIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllByIdInAndMemberId(readIds, memberId);
            notifications.forEach(Notification::markAsRead);
        }
    }
}