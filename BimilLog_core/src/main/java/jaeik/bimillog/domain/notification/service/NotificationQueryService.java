package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.notification.controller.NotificationQueryController;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * <h2>알림 조회 서비스</h2>
 * <p>알림 도메인의 조회 작업을 담당하는 서비스입니다.</p>
 * <p>알림 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    /**
     * <h3>알림 목록 조회</h3>
     * <p>현재 로그인한 사용자의 모든 알림을 최신순으로 조회합니다.</p>
     * <p>사용자 인증 정보가 유효하지 않은 경우 빈 목록을 반환합니다.</p>
     * <p>{@link NotificationQueryController}에서 사용자의 알림함 조회 API 요청 시 호출됩니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 알림 엔티티 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationList(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMemberId() == null) {
            return Collections.emptyList();
        }

        List<Notification> notifications = notificationRepository.getNotificationList(userDetails.getMemberId());

        return notifications != null ? notifications : Collections.emptyList();
    }
}