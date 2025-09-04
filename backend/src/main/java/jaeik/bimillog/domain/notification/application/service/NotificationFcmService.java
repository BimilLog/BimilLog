package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.application.port.out.LoadUserPort;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
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
    private final LoadUserPort loadUserPort;

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

        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new NotificationCustomException(NotificationErrorCode.NOTIFICATION_USER_NOT_FOUND));

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
            List<FcmToken> fcmTokens = fcmPort.findValidFcmTokensForCommentNotification(postUserId);
            if (fcmTokens.isEmpty()) {
                log.info("댓글 알림이 비활성화되어 있거나 FCM 토큰이 없어 알림을 전송하지 않습니다. 사용자 ID={}", postUserId);
                return;
            }

            String title = commenterName + "님이 댓글을 남겼습니다!";
            String body = "지금 확인해보세요!";
            
            for (FcmToken token : fcmTokens) {
                FcmMessage fcmMessage = FcmMessage.of(
                        token.getFcmRegistrationToken(),
                        title,
                        body
                );
                fcmPort.sendMessageTo(fcmMessage);
            }
            log.info("댓글 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", postUserId, fcmTokens.size());
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
            List<FcmToken> fcmTokens = fcmPort.findValidFcmTokensForMessageNotification(farmOwnerId);
            if (fcmTokens.isEmpty()) {
                log.info("메시지 알림이 비활성화되어 있거나 FCM 토큰이 없어 알림을 전송하지 않습니다. 사용자 ID={}", farmOwnerId);
                return;
            }

            String title = "롤링페이퍼에 메시지가 작성되었어요!";
            String body = "지금 확인해보세요!";
            
            for (FcmToken token : fcmTokens) {
                FcmMessage fcmMessage = FcmMessage.of(
                        token.getFcmRegistrationToken(),
                        title,
                        body
                );
                fcmPort.sendMessageTo(fcmMessage);
            }
            log.info("롤링페이퍼 메시지 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", farmOwnerId, fcmTokens.size());
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
            List<FcmToken> fcmTokens = fcmPort.findValidFcmTokensForPostFeaturedNotification(userId);
            if (fcmTokens.isEmpty()) {
                log.info("인기글 알림이 비활성화되어 있거나 FCM 토큰이 없어 알림을 전송하지 않습니다. 사용자 ID={}", userId);
                return;
            }
            
            for (FcmToken token : fcmTokens) {
                FcmMessage fcmMessage = FcmMessage.of(
                        token.getFcmRegistrationToken(),
                        title,
                        body
                );
                fcmPort.sendMessageTo(fcmMessage);
            }
            log.info("인기글 등극 알림 FCM 전송 완료: 사용자 ID={}, 토큰 수={}", userId, fcmTokens.size());
        } catch (Exception e) {
            log.error("FCM 인기글 등극 알림 전송 실패: 사용자 ID={}, 제목={}", userId, title, e);
        }
    }
}
