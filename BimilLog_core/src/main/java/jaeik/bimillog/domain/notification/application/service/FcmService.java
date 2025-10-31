package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.infrastructure.adapter.in.notification.listener.NotificationGenerateListener;
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
public class FcmService implements FcmUseCase {

    private final FcmPort fcmPort;
    private final NotificationUtilPort notificationUtilPort;

    /**
     * <h3>FCM 토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제하거나 회원탈퇴시 모든 토큰을 삭제합니다.</p>
     * <p>fcmTokenId가 null인 경우 모든 토큰 삭제, 값이 있는 경우 특정 토큰만 삭제합니다.</p>
     * <p>MemberLogoutListener, MemberWithdrawListener, UserBannedListener에서 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param fcmTokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteFcmTokens(Long memberId, Long fcmTokenId) {
        fcmPort.deleteFcmTokens(memberId, fcmTokenId);
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
            boolean sent = sendNotifications(tokens, title, body);
            if (sent) {
                log.info("댓글 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", postUserId, tokens.size());
            }
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
            List<FcmToken> tokens = notificationUtilPort.FcmEligibleFcmTokens(farmOwnerId, NotificationType.MESSAGE);
            String title = "롤링페이퍼에 메시지가 작성되었어요!";
            String body = "지금 확인해보세요!";
            boolean sent = sendNotifications(tokens, title, body);
            if (sent) {
                log.info("롤링페이퍼 메시지 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", farmOwnerId, tokens.size());
            }
        } catch (Exception e) {
            log.error("FCM 롤링페이퍼 알림 전송 실패: 롤링페이퍼 주인 ID={}", farmOwnerId, e);
        }
    }

    /**
     * <h3>인기글 등극 알림 FCM 전송</h3>
     * <p>게시글이 인기글로 선정되었을 때 게시글 작성자에게 FCM 푸시 알림을 전송합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param title  알림 제목
     * @param body   알림 내용
     * @see NotificationGenerateListener
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendPostFeaturedNotification(Long memberId, String title, String body) {
        try {
            List<FcmToken> tokens = notificationUtilPort.FcmEligibleFcmTokens(memberId, NotificationType.POST_FEATURED);
            boolean sent = sendNotifications(tokens, title, body);
            if (sent) {
                log.info("인기글 등극 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", memberId, tokens.size());
            }
        } catch (Exception e) {
            log.error("FCM 인기글 등극 알림 전송 실패: 사용자 ID={}, 제목={}", memberId, title, e);
        }
    }


    /**
     * <h3>FCM 알림 전송 헬퍼 메서드</h3>
     * <p>FCM 토큰 목록을 순회하며 개별 푸시 메시지를 전송하는 내부 유틸리티 메서드입니다.</p>
     * <p>개별 메시지 전송 실패 시에도 전체 전송을 중단하지 않고 계속 진행하여 가용성을 보장합니다.</p>
     * <p>비어있는 토큰 목록에 대해서는 알림 전송을 건너뜁니다.</p>
     *
     * @param tokens 전송 대상 FCM 토큰 목록
     * @param title  메시지 제목
     * @param body   메시지 본문
     * @return 실제로 알림을 전송했는지 여부 (토큰이 없으면 false, 있으면 true)
     */
    private boolean sendNotifications(List<FcmToken> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            log.debug("전송할 FCM 토큰이 없습니다. 알림을 전송하지 않습니다.");
            return false;
        }

        for (FcmToken token : tokens) {
            try {
                fcmPort.sendMessageTo(FcmMessage.of(token.getFcmRegistrationToken(), title, body));
            } catch (Exception e) {
                // 개별 메시지 전송 실패 시에도 전체 전송을 중단하지 않도록 함
                log.warn("FCM 메시지 전송 중 일부 실패: 토큰={}", token.getFcmRegistrationToken(), e);
            }
        }
        return true;
    }
}
