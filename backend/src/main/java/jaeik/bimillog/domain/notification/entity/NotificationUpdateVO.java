package jaeik.bimillog.domain.notification.entity;

import lombok.Builder;

import java.util.List;

/**
 * <h3>알림 업데이트 명령 값 객체</h3>
 * <p>
 * 알림 읽음/삭제 처리를 위한 도메인 순수 값 객체
 * UpdateNotificationDTO의 도메인 전용 대체
 * </p>
 *
 * @param readIds 읽음 처리할 알림 ID 목록
 * @param deletedIds 삭제할 알림 ID 목록
 * @author Jaeik
 * @since 2.0.0
 */
public record NotificationUpdateVO(
        List<Long> readIds,
        List<Long> deletedIds
) {

    /**
     * <h3>빌더를 통한 생성자</h3>
     * <p>Record의 컴팩트 생성자입니다. 빌더 패턴을 지원합니다.</p>
     */
    @Builder
    public NotificationUpdateVO {
    }

    /**
     * <h3>알림 업데이트 명령 생성</h3>
     * <p>읽음 처리할 ID와 삭제할 ID 목록으로 명령을 생성합니다.</p>
     *
     * @param readIds 읽음 처리할 알림 ID 목록
     * @param deletedIds 삭제할 알림 ID 목록
     * @return NotificationUpdateVO 값 객체
     */
    public static NotificationUpdateVO of(List<Long> readIds, List<Long> deletedIds) {
        return NotificationUpdateVO.builder()
                .readIds(readIds)
                .deletedIds(deletedIds)
                .build();
    }
}