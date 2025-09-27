package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
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
 * <h2>FCM 푸시 알림 서비스</h2>
 * <p>FCM 토큰 관리와 푸시 알림 전송을 담당하는 서비스입니다.</p>
 * <p>FCM 토큰 등록/삭제, 댓글 알림, 롤링페이퍼 알림, 인기글 알림</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFcmService implements NotificationFcmUseCase {

    private final FcmPort fcmPort;
    private final NotificationUtilPort notificationUtilPort;

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>클라이언트에서 전송한 FCM 토큰을 서버에 등록하여 푸시 알림 수신을 준비합니다.</p>
     * <p>중복 토큰 검사, 사용자 존재성 확인, 다중 기기 지원을 통해 안정적인 토큰 관리를 수행합니다.</p>
     * <p>NotificationFcmController에서 클라이언트의 토큰 등록 API 요청을 처리하기 위해 호출됩니다.</p>
     *
     * @param user   사용자
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @return 저장된 FCM 토큰 엔티티의 ID (토큰이 없거나 빈 값인 경우 null)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Long registerFcmToken(User user, String fcmToken) {
        if (user == null) {
            throw new NotificationCustomException(NotificationErrorCode.NOTIFICATION_USER_NOT_FOUND);
        }

        log.info("FCM 토큰 등록 처리 시작: 사용자 ID={}", user.getId());

        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM 토큰이 비어있습니다. 사용자 ID={}", user.getId());
            return null;
        }

        FcmToken savedToken = fcmPort.save(FcmToken.create(user, fcmToken));
        log.info("FCM 토큰 등록 완료: 사용자 ID={}, 토큰 ID={}", user.getId(), savedToken.getId());
        return savedToken.getId();
    }

    /**
     * <h3>FCM 토큰 삭제 처리</h3>
     * <p>사용자 탈퇴 시 개인정보 보호를 위해 해당 사용자의 모든 FCM 토큰을 삭제합니다.</p>
     * <p>다중 기기에 등록된 모든 토큰을 일괄적으로 제거하여 더 이상 푸시 알림이 전송되지 않도록 합니다.</p>
     * <p>NotificationRemoveListener에서 사용자 탈퇴 이벤트 발생 시 호출됩니다.</p>
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
     * <p>댓글 작성 완료 시 게시글 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>알림 수신 자격 검증을 거쳐 유효한 FCM 토큰에만 알림을 발송하며, 전송 실패 시에도 예외를 발생시키지 않습니다.</p>
     * <p>CommentNotificationListener에서 댓글 작성 이벤트 발생 시 호출됩니다.</p>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendCommentNotification(Long postUserId, String commenterName) {
        try {
            List<FcmToken> tokens = notificationUtilPort.FcmEligibleFcmTokens(postUserId, NotificationType.COMMENT);
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
     * <p>롤링페이퍼에 새 메시지 작성 완료 시 롤링페이퍼 소유자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>알림 수신 자격 검증을 거쳐 유효한 FCM 토큰에만 알림을 발송하며, 전송 실패 시에도 예외를 발생시키지 않습니다.</p>
     * <p>PaperNotificationListener에서 롤링페이퍼 메시지 작성 이벤트 발생 시 호출됩니다.</p>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendPaperPlantNotification(Long farmOwnerId) {
        try {
            List<FcmToken> tokens = notificationUtilPort.FcmEligibleFcmTokens(farmOwnerId, NotificationType.PAPER);
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
     * <p>게시글이 인기글로 선정되었을 때 게시글 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     * <p>알림 수신 자격 검증을 거쳐 유효한 FCM 토큰에만 알림을 발송하며, 전송 실패 시에도 예외를 발생시키지 않습니다.</p>
     * <p>PostFeaturedListener에서 인기글 등극 이벤트 발생 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param title  알림 제목
     * @param body   알림 내용
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendPostFeaturedNotification(Long userId, String title, String body) {
        try {
            List<FcmToken> tokens = notificationUtilPort.FcmEligibleFcmTokens(userId, NotificationType.POST_FEATURED);
            sendNotifications(tokens, title, body);
            log.info("인기글 등극 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", userId, tokens.size());
        } catch (Exception e) {
            log.error("FCM 인기글 등극 알림 전송 실패: 사용자 ID={}, 제목={}", userId, title, e);
        }
    }

    /**
     * <h3>FCM 알림 전송 도우미 메서드</h3>
     * <p>FCM 토큰 목록을 순회하며 개별 푸시 메시지를 전솨하는 내부 유틸리티 메서드입니다.</p>
     * <p>개별 메시지 전송 실패 시에도 전체 전송을 중단하지 않고 계속 진행하여 가용성을 보장합니다.</p>
     * <p>비어있은 토큰 목록에 대해서는 알림 전송을 건너뜀니다.</p>
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
