package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.event.AlarmSendEvent;
import jaeik.bimillog.domain.notification.listener.NotificationGenerateListener;
import jaeik.bimillog.domain.notification.repository.NotificationUtilRepository;
import jaeik.bimillog.infrastructure.api.fcm.FcmAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>FCM 푸시 알림 서비스</h2>
 * <p>FCM 토큰 관리와 푸시 알림 전송을 담당하는 서비스입니다.</p>
 * <p>알림 타입별로 적절한 제목과 내용을 조립하여 FCM 푸시 알림을 전송합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmCommandService {
    private final FcmAdapter fcmAdapter;
    private final NotificationUtilRepository notificationUtilRepository;

    /**
     * <h3>FCM 푸시 알림 전송 (범용)</h3>
     * <p>알림 타입별로 적절한 제목과 내용을 조립하여 FCM 푸시 알림을 전송합니다.</p>
     * <p>알림 수신 자격 검증을 거쳐 유효한 FCM 토큰에만 알림을 발송하며, 전송 실패 시에도 예외를 발생시키지 않습니다.</p>
     *
     * @see NotificationGenerateListener
     * @author Jaeik
     * @since 2.0.0
     */
    public void sendNotification(NotificationType type, Long memberId, String commenterName, String postTitle) {
        try {
            String title = "";
            String body = "";

            // 알림 타입별 FCM 메시지 조립
            switch (type) {
                case COMMENT -> {
                    title = commenterName + "님이 댓글을 남겼습니다!";
                    body = "지금 확인해보세요!";
                }
                case MESSAGE -> {
                    title = "롤링페이퍼에 메시지가 작성되었어요!";
                    body = "지금 확인해보세요!";
                }
                case POST_FEATURED_WEEKLY -> {
                    title = "축하합니다! 주간 인기글에 선정되었습니다!";
                    body = String.format("회원님의 게시글 %s 이 주간 인기 게시글로 선정되었습니다.", postTitle);
                }
                case POST_FEATURED_LEGEND -> {
                    title = "축하합니다! 명예의 전당에 등극했습니다!";
                    body = String.format("회원님의 게시글 %s 이 명예의 전당에 등극했습니다.", postTitle);
                }
                case POST_FEATURED_REALTIME -> {
                    title = "축하합니다! 실시간 인기글에 선정되었습니다!";
                    body = String.format("회원님의 게시글 %s 이 실시간 인기글로 선정되었습니다.", postTitle);
                }
                case FRIEND -> {
                    title = "새로운 친구 요청이 도착했어요!";
                    body = String.format("%s님 에게서 친구 요청이 도착했습니다.", commenterName);
                }
            }

            List<String> tokens = notificationUtilRepository.FcmEligibleFcmTokens(
                    memberId,
                    type
            );

            boolean sent = sendNotifications(tokens, title, body);
            if (sent) {
                log.info("{} 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}",
                        type, memberId, tokens.size());
            }
        } catch (Exception e) {
            log.error("FCM {} 알림 전송 실패: 사용자 ID={}",
                    type, memberId, e);
        }
    }


    /**
     * <h3>FCM 알림 전송 헬퍼 메서드</h3>
     * <p>FCM 토큰 목록을 순회하며 개별 푸시 메시지를 전송하는 내부 유틸리티 메서드입니다.</p>
     * <p>개별 메시지 전송 실패 시에도 전체 전송을 중단하지 않고 계속 진행하여 가용성을 보장합니다.</p>
     * <p>비어있는 토큰 목록에 대해서는 알림 전송을 건너뜁니다.</p>
     *
     * @param tokens 전송 대상 FCM 토큰 문자열 목록
     * @param title  메시지 제목
     * @param body   메시지 본문
     * @return 실제로 알림을 전송했는지 여부 (토큰이 없으면 false, 있으면 true)
     */
    private boolean sendNotifications(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            log.debug("전송할 FCM 토큰이 없습니다. 알림을 전송하지 않습니다.");
            return false;
        }

        for (String token : tokens) {
            try {
                fcmAdapter.sendMessageTo(FcmMessage.of(token, title, body));
            } catch (Exception e) {
                // 개별 메시지 전송 실패 시에도 전체 전송을 중단하지 않도록 함
                log.warn("FCM 메시지 전송 중 일부 실패: 토큰={}", token, e);
            }
        }
        return true;
    }
}
