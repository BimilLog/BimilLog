package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Users;
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
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final NotificationService notificationService;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * <h3>댓글 작성 FCM 알림 (비동기)</h3>
     *
     * @param postOwner     게시글 작성자
     * @param commenterName 댓글 작성자 이름
     */
    @Async("fcmNotificationExecutor")
    public void sendCommentFcmNotificationAsync(Users postOwner, String commenterName) {
        try {
            log.info("댓글 FCM 알림 비동기 처리 시작: userId={}, 스레드={}",
                    postOwner.getId(), Thread.currentThread().getName());

            if (postOwner.getSetting().isCommentNotification()) {
                List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(postOwner);
                for (FcmToken fcmToken : fcmTokens) {
                    notificationService.sendMessageTo(FcmSendDTO.builder()
                            .token(fcmToken.getFcmRegistrationToken())
                            .title(commenterName + "님이 댓글을 남겼습니다!")
                            .body("지금 확인해보세요!")
                            .build());
                }
                log.info("댓글 FCM 알림 비동기 처리 완료: userId={}, 토큰 수={}",
                        postOwner.getId(), fcmTokens.size());
            } else {
                log.info("댓글 FCM 알림 설정 비활성화: userId={}", postOwner.getId());
            }

        } catch (Exception e) {
            log.error("댓글 FCM 알림 비동기 처리 실패: userId={}, error={}",
                    postOwner.getId(), e.getMessage());
        }
    }

    /**
     * <h3>농작물 심기 FCM 알림 (비동기)</h3>
     *
     * @param farmOwner 농장 주인
     */
    @Async("fcmNotificationExecutor")
    public void sendFarmPlantFcmNotificationAsync(Users farmOwner) {
        try {
            log.info("농작물 심기 FCM 알림 비동기 처리 시작: userId={}, 스레드={}",
                    farmOwner.getId(), Thread.currentThread().getName());

            if (farmOwner.getSetting().isFarmNotification()) {
                List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(farmOwner);
                for (FcmToken fcmToken : fcmTokens) {
                    notificationService.sendMessageTo(FcmSendDTO.builder()
                            .token(fcmToken.getFcmRegistrationToken())
                            .title("누군가가 농장에 농작물을 심었습니다!")
                            .body("지금 확인해보세요!")
                            .build());
                }
                log.info("농작물 심기 FCM 알림 비동기 처리 완료: userId={}, 토큰 수={}",
                        farmOwner.getId(), fcmTokens.size());
            } else {
                log.info("농작물 심기 FCM 알림 설정 비활성화: userId={}", farmOwner.getId());
            }

        } catch (Exception e) {
            log.error("농작물 심기 FCM 알림 비동기 처리 실패: userId={}, error={}",
                    farmOwner.getId(), e.getMessage());
        }
    }

    /**
     * <h3>인기글 등극 FCM 알림 (비동기)</h3>
     *
     * @param user  사용자
     * @param title 알림 제목
     * @param body  알림 내용
     */
    @Async("fcmNotificationExecutor")
    public void sendPostFeaturedFcmNotificationAsync(Users user, String title, String body) {
        try {
            log.info("인기글 FCM 알림 비동기 처리 시작: userId={}, 스레드={}",
                    user.getId(), Thread.currentThread().getName());

            if (user.getSetting().isPostFeaturedNotification()) {
                List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(user);
                for (FcmToken fcmToken : fcmTokens) {
                    notificationService.sendMessageTo(FcmSendDTO.builder()
                            .token(fcmToken.getFcmRegistrationToken())
                            .title(title)
                            .body(body)
                            .build());
                }
                log.info("인기글 FCM 알림 비동기 처리 완료: userId={}, 토큰 수={}",
                        user.getId(), fcmTokens.size());
            } else {
                log.info("인기글 FCM 알림 설정 비활성화: userId={}", user.getId());
            }

        } catch (Exception e) {
            log.error("인기글 FCM 알림 비동기 처리 실패: userId={}, error={}",
                    user.getId(), e.getMessage());
        }
    }

    /**
     * <h3>인기댓글 등극 FCM 알림 (비동기)</h3>
     *
     * @param user 사용자
     */
    @Async("fcmNotificationExecutor")
    public void sendCommentFeaturedFcmNotificationAsync(Users user) {
        try {
            log.info("인기댓글 FCM 알림 비동기 처리 시작: userId={}, 스레드={}",
                    user.getId(), Thread.currentThread().getName());

            if (user.getSetting().isCommentFeaturedNotification()) {
                List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(user);
                for (FcmToken fcmToken : fcmTokens) {
                    notificationService.sendMessageTo(FcmSendDTO.builder()
                            .token(fcmToken.getFcmRegistrationToken())
                            .title("🎉 당신의 댓글이 인기 댓글로 선정되었습니다!")
                            .body("지금 확인해보세요!")
                            .build());
                }
                log.info("인기댓글 FCM 알림 비동기 처리 완료: userId={}, 토큰 수={}",
                        user.getId(), fcmTokens.size());
            } else {
                log.info("인기댓글 FCM 알림 설정 비활성화: userId={}", user.getId());
            }

        } catch (Exception e) {
            log.error("인기댓글 FCM 알림 비동기 처리 실패: userId={}, error={}",
                    user.getId(), e.getMessage());
        }
    }
}