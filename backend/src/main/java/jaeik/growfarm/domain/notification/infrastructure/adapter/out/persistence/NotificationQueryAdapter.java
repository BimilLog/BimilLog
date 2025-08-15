package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationQueryAdapter implements NotificationQueryPort {

    private final NotificationReadRepository notificationReadRepository;


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
        return notificationReadRepository.findNotificationsByUserIdOrderByLatest(userDetails.getUserId());
    }
}
