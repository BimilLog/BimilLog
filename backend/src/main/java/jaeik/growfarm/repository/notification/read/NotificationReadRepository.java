package jaeik.growfarm.repository.notification.read;

import jaeik.growfarm.dto.notification.NotificationDTO;

import java.util.List;

/**
 * <h2>알림 읽기 레포지터리 인터페이스</h2>
 * <p>
 * 알림 조회 관련 기능만 담당하는 인터페이스
 * SRP: 알림 조회만 담당
 * ISP: 읽기 기능만 노출
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public interface NotificationReadRepository {

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
    List<NotificationDTO> findNotificationsByUserIdOrderByLatest(Long userId);

}