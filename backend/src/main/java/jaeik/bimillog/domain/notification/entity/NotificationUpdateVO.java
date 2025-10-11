package jaeik.bimillog.domain.notification.entity;

import java.util.List;

/**
 * <h2>알림 업데이트 명령 값 객체</h2>
 * <p>
 * 알림 읽음/삭제 처리를 위한 도메인 순수 값 객체
 * </p>
 * <p>사용자가 알림 목록에서 읽음 처리나 삭제 요청 시 NotificationCommandController에서 요청되어 일괄 업데이트 처리를 위한 명령 데이터를 담는 클래스</p>
 * <p>UpdateNotificationDTO의 도메인 전용 대체</p>
 *
 * @param readIds 읽음 처리할 알림 ID 목록
 * @param deletedIds 삭제할 알림 ID 목록
 * @author Jaeik
 * @version 2.0.0
 */
public record NotificationUpdateVO(
        List<Long> readIds,
        List<Long> deletedIds
) {

    /**
     * <h3>알림 업데이트 명령 생성</h3>
     * <p>읽음 처리할 ID와 삭제할 ID 목록으로 명령을 생성합니다.</p>
     * <p>사용자가 알림 목록에서 일괄 읽음 처리나 선택 삭제 요청 시 NotificationCommandController에서 호출되어 업데이트 명령 객체를 생성하는 메서드</p>
     *
     * @param readIds 읽음 처리할 알림 ID 목록
     * @param deletedIds 삭제할 알림 ID 목록
     * @return NotificationUpdateVO 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static NotificationUpdateVO of(List<Long> readIds, List<Long> deletedIds) {
        return new NotificationUpdateVO(readIds, deletedIds);
    }
}