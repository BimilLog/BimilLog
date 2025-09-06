package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationToUserPort;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>FCM 토큰 관리 서비스</h2>
 * <p>FCM 토큰 등록 및 삭제 관련 비즈니스 로직을 처리하는 사용 사례 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFcmService implements NotificationFcmUseCase {

    private final FcmPort fcmPort;
    private final NotificationToUserPort notificationToUserPort;

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>사용자 로그인 또는 회원가입 시 FCM 토큰을 등록합니다.</p>
     *
     * @param userId   사용자 ID
     * @param fcmToken FCM 토큰 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void registerFcmToken(Long userId, String fcmToken) {
        log.info("FCM 토큰 등록 처리 시작: 사용자 ID={}", userId);

        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM 토큰이 비어있습니다. 사용자 ID={}", userId);
            return;
        }

        User user = notificationToUserPort.findById(userId);

        fcmPort.save(FcmToken.create(user, fcmToken));
    }

    /**
     * <h3>FCM 토큰 삭제 처리</h3>
     * <p>사용자 로그아웃 또는 탈퇴 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteFcmTokens(Long userId) {
        log.info("FCM 토큰 삭제 처리 시작: 사용자 ID={}", userId);
        fcmPort.deleteByUserId(userId);
    }

    /**
     * <h3>댓글 알림 FCM 전송</h3>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     */
    @Override
    public void sendCommentNotification(Long postUserId, String commenterName) {
        try {
            List<FcmToken> tokens = fcmPort.findValidFcmTokensByNotificationType(postUserId, NotificationType.COMMENT);
            String title = commenterName + "님이 댓글을 남겼습니다!";
            String body = "지금 확인해보세요!";
            sendNotifications(tokens, title, body);
            log.info("댓글 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", postUserId, tokens.size());
        } catch (Exception e) {
            log.error("FCM 댓글 알림 전송 실패: 사용자 ID={}, 댓글작성자={}", postUserId, commenterName, e);
        }
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 FCM 전송</h3>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     */
    @Override
    public void sendPaperPlantNotification(Long farmOwnerId) {
        try {
            List<FcmToken> tokens = fcmPort.findValidFcmTokensByNotificationType(farmOwnerId, NotificationType.PAPER);
            String title = "롤링페이퍼에 메시지가 작성되었어요!";
            String body = "지금 확인해보세요!";
            sendNotifications(tokens, title, body);
            log.info("롤링페이퍼 메시지 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", farmOwnerId, tokens.size());
        } catch (Exception e) {
            log.error("FCM 롤링페이퍼 알림 전송 실패: 롤링페이퍼 주인 ID={}", farmOwnerId, e);
        }
    }

    /**
     * <h3>인기글 등극 알림 FCM 전송</h3>
     *
     * @param userId 사용자 ID
     * @param title  알림 제목
     * @param body   알림 내용
     */
    @Override
    public void sendPostFeaturedNotification(Long userId, String title, String body) {
        try {
            List<FcmToken> tokens = fcmPort.findValidFcmTokensByNotificationType(userId, NotificationType.POST_FEATURED);
            sendNotifications(tokens, title, body);
            log.info("인기글 등극 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", userId, tokens.size());
        } catch (Exception e) {
            log.error("FCM 인기글 등극 알림 전송 실패: 사용자 ID={}, 제목={}", userId, title, e);
        }
    }

    /**
     * <h3>FCM 알림 전송 도우미 메서드</h3>
     * <p>토큰 목록을 받아 FCM 메시지를 전송하는 공통 로직을 처리합니다.</p>
     *
     * @param tokens 전송 대상 FCM 토큰 목록
     * @param title  메시지 제목
     * @param body   메시지 본문
     */
    private void sendNotifications(List<FcmToken> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            log.debug("전송할 FCM 토큰이 없습니다. 알림을 전송하지 않습니다.");
            return;
        }

        for (FcmToken token : tokens) {
            try {
                fcmPort.sendMessageTo(FcmMessage.of(token.getFcmRegistrationToken(), title, body));
            } catch (Exception e) {
                // 개별 메시지 전송 실패 시에도 전체 전송을 중단하지 않도록 함
                log.warn("FCM 메시지 전송 중 일부 실패: 토큰={}", token.getFcmRegistrationToken(), e);
            }
        }
    }
}
