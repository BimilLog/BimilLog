package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
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
    private final UserRepository userRepository;

    /**
     * <h3>댓글 달림 FCM 알림</h3>
     *
     * @param postOwnerId 게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendCommentFcmNotificationAsync(Long postOwnerId, String commenterName) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(postOwnerId);
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
     * @param paperOwnerId 롤링페이퍼 주인 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendPaperPlantFcmNotificationAsync(Long paperOwnerId) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(paperOwnerId);
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
     * @param userId 사용자 ID
     * @param title 알림 제목
     * @param body  알림 내용
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendPostFeaturedFcmNotificationAsync(Long userId, String title, String body) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(userId);
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
     * @param userId 사용자 ID
     * @return 유효한 FCM 토큰 리스트 또는 null
     * @author Jaeik
     * @since 1.0.0
     */
    public List<FcmToken> fcmValidate(Long userId) {
        List<FcmToken> fcmTokens = fcmTokenRepository.findValidFcmTokensByUserId(userId);
        return fcmTokens.isEmpty() ? null : fcmTokens;
    }
}