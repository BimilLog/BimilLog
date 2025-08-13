package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import jaeik.growfarm.domain.notification.application.port.out.DeleteFcmTokenPort;
import jaeik.growfarm.domain.notification.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.notification.application.port.out.SaveFcmTokenPort;
import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>알림 이벤트 서비스</h2>
 * <p>알림 관련 이벤트를 처리하는 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationEventService implements NotificationEventUseCase {

    private final SaveFcmTokenPort saveFcmTokenPort;
    private final DeleteFcmTokenPort deleteFcmTokenPort;
    private final LoadUserPort loadUserPort;

    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        // TODO: 댓글 알림 전송 구현
        log.info("댓글 알림 전송: postUserId={}, commenterName={}, postId={}", postUserId, commenterName, postId);
    }

    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        // TODO: 롤링페이퍼 메시지 알림 전송 구현
        log.info("롤링페이퍼 메시지 알림 전송: farmOwnerId={}, userName={}", farmOwnerId, userName);
    }

    @Override
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        // TODO: 인기글 등극 알림 전송 구현
        log.info("인기글 등극 알림 전송: userId={}, message={}, postId={}", userId, message, postId);
    }

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
        
        saveFcmTokenPort.save(FcmToken.create(user, fcmToken));
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
        deleteFcmTokenPort.deleteByUserId(userId);
    }
}