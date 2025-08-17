package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.notification.application.port.out.FcmPort;
import jaeik.growfarm.domain.notification.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.notification.out.fcm.dto.FcmSendDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;


/**
 * <h2>FCM 토큰 관리 서비스</h2>
 * <p>FCM 토큰 등록 및 삭제 관련 비즈니스 로직을 처리하는 Use Case 구현</p>
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
        log.info("FCM 토큰 등록 처리: userId={}", userId);

        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM 토큰이 비어있습니다. userId={}", userId);
            return;
        }

        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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
        log.info("FCM 토큰 삭제 처리: userId={}", userId);
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
                log.info("댓글 알림이 비활성화되어 있거나 FCM 토큰이 없어 알림을 전송하지 않습니다. userId={}", postUserId);
                return;
            }

            String title = commenterName + "님이 댓글을 남겼습니다!";
            String body = "지금 확인해보세요!";
            
            for (FcmToken token : fcmTokens) {
                FcmSendDTO fcmSendDto = FcmSendDTO.builder()
                        .token(token.getFcmRegistrationToken())
                        .title(title)
                        .body(body)
                        .build();
                fcmPort.sendMessageTo(fcmSendDto);
            }
            log.info("댓글 알림 FCM 전송 완료: userId={}, tokenCount={}", postUserId, fcmTokens.size());
        } catch (Exception e) {
            log.error("FCM 댓글 알림 전송 실패: userId={}, commenterName={}", postUserId, commenterName, e);
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
                log.info("메시지 알림이 비활성화되어 있거나 FCM 토큰이 없어 알림을 전송하지 않습니다. userId={}", farmOwnerId);
                return;
            }

            String title = "롤링페이퍼에 메시지가 작성되었어요!";
            String body = "지금 확인해보세요!";
            
            for (FcmToken token : fcmTokens) {
                FcmSendDTO fcmSendDto = FcmSendDTO.builder()
                        .token(token.getFcmRegistrationToken())
                        .title(title)
                        .body(body)
                        .build();
                fcmPort.sendMessageTo(fcmSendDto);
            }
            log.info("롤링페이퍼 메시지 알림 FCM 전송 완료: userId={}, tokenCount={}", farmOwnerId, fcmTokens.size());
        } catch (Exception e) {
            log.error("FCM 롤링페이퍼 알림 전송 실패: farmOwnerId={}", farmOwnerId, e);
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
                log.info("인기글 알림이 비활성화되어 있거나 FCM 토큰이 없어 알림을 전송하지 않습니다. userId={}", userId);
                return;
            }
            
            for (FcmToken token : fcmTokens) {
                FcmSendDTO fcmSendDto = FcmSendDTO.builder()
                        .token(token.getFcmRegistrationToken())
                        .title(title)
                        .body(body)
                        .build();
                fcmPort.sendMessageTo(fcmSendDto);
            }
            log.info("인기글 등극 알림 FCM 전송 완료: userId={}, tokenCount={}", userId, fcmTokens.size());
        } catch (Exception e) {
            log.error("FCM 인기글 등극 알림 전송 실패: userId={}, title={}", userId, title, e);
        }
    }
}
