package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.listener.NotificationSendListener;
import jaeik.bimillog.domain.notification.repository.NotificationQueryRepository;
import jaeik.bimillog.infrastructure.api.fcm.FcmAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>FCM 푸시 알림 서비스</h2>
 * <p>FCM 토큰 관리와 푸시 알림 전송을 담당하는 서비스입니다.</p>
 *
 * @author Jaeik
 * @version 2.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmCommandService {
    private final FcmAdapter fcmAdapter;
    private final NotificationQueryRepository notificationQueryRepository;

    /**
     * <h3>FCM 푸시 알림 전송</h3>
     * <p>FCM 푸시 알림을 전송합니다.</p>
     * <p>알림 수신 자격 검증을 거쳐 유효한 FCM 토큰에만 알림을 발송하며, 다음 발송을 진행.</p>
     *
     * @author Jaeik
     * @see NotificationSendListener
     * @since 2.3.0
     */
    public void sendNotification(NotificationType type, Long memberId, String relatedMemberName, String postTitle) {
        List<String> tokens = notificationQueryRepository.fcmEligibleFcmTokens(memberId, type);
        if (tokens.isEmpty()) {
            log.debug("전송할 토큰이 없습니다.");
            return;
        }

        String title = type.getFCMTitle(relatedMemberName);
        String body = type.getFCMBody(relatedMemberName, postTitle);

        for (String token : tokens) {
            try {
                fcmAdapter.sendMessageTo(token, title, body);
            } catch (Exception e) {
                log.error("{} FCM 알림 전송 실패: 사용자 ID={}", type, memberId, e);
            }
        }
    }
}
