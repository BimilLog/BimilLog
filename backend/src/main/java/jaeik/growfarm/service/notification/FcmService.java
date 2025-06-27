package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>비동기 FCM 알림 서비스</h2>
 * <p>
 * FCM 푸시 알림을 비동기로 처리하는 전용 서비스
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final NotificationService notificationService;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * <h3>댓글 달림 FCM 알림</h3>
     *
     * @param postOwner     게시글 작성자
     * @param commenterName 댓글 작성자 이름
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendCommentFcmNotificationAsync(Users postOwner, String commenterName) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(postOwner);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                notificationService.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(commenterName + "님이 댓글을 남겼습니다!")
                        .body("지금 확인해보세요!")
                        .build());
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>롤링페이퍼에 메시지 수신 FCM 알림</h3>
     *
     * @param farmOwner 농장 주인
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendPaperPlantFcmNotificationAsync(Users farmOwner) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(farmOwner);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                notificationService.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title("롤링페이퍼에 메시지가 작성되었어요!")
                        .body("지금 확인해보세요!")
                        .build());
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>인기글 등극 FCM 알림</h3>
     *
     * @param user  사용자
     * @param title 알림 제목
     * @param body  알림 내용
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendPostFeaturedFcmNotificationAsync(Users user, String title, String body) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(user);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                notificationService.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(title)
                        .body(body)
                        .build());
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>FCM 토큰 유효성 검사</h3>
     * <p>
     * 사용자 설정에 따라 FCM 토큰을 조회하고 유효성을 검사합니다.
     * </p>
     *
     * @param user 사용자 정보
     * @return 유효한 FCM 토큰 리스트 또는 null
     * @author Jaeik
     * @since 1.0.0
     */
    public List<FcmToken> fcmValidate(Users user) {
        if (!user.getSetting().isMessageNotification()) {
            return null;
        }

        List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(user);

        if (fcmTokens == null || fcmTokens.isEmpty()) {
            return null;
        }
        return fcmTokens;
    }
}