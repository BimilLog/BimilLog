package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.notification.application.port.out.FcmPort;
import jaeik.growfarm.domain.notification.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


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
}
