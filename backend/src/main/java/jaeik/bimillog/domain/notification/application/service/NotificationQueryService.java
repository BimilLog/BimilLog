package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationQueryUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.bimillog.domain.notification.entity.NotificationInfo;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * <h2>알림 조회 서비스</h2>
 * <p>알림 조회 관련 비즈니스 로직을 처리하는 Use Case 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationQueryPort notificationQueryPort;

    /**
     * <h3>알림 목록 조회</h3>
     * <p>현재 로그인한 사용자의 알림 목록을 조회합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 알림 DTO 목록 (null 안전성 보장 - 빈 리스트 반환)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationInfo> getNotificationList(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return Collections.emptyList();
        }
        
        List<NotificationInfo> notifications = notificationQueryPort.getNotificationList(userDetails.getUserId());
        
        return notifications != null ? notifications : Collections.emptyList();
    }
}