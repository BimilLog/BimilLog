package jaeik.growfarm.domain.notification.infrastructure.listener;

import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.FcmTokenRepository;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.event.FcmTokenRegisteredEvent;
import jaeik.growfarm.global.event.UserLoggedOutEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>FCM 토큰 이벤트 리스너</h2>
 * <p>FCM 토큰 관련 이벤트를 처리하는 리스너</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmTokenEventListener {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>FCM 토큰 등록 이벤트 처리</h3>
     * <p>사용자 로그인 또는 회원가입 시 FCM 토큰을 등록합니다.</p>
     *
     * @param event FCM 토큰 등록 이벤트
     */
    @EventListener
    @Transactional
    public void handleFcmTokenRegisteredEvent(FcmTokenRegisteredEvent event) {
        log.info("FCM 토큰 등록 이벤트 처리: userId={}", event.userId());
        
        if (event.fcmToken() == null || event.fcmToken().isEmpty()) {
            log.warn("FCM 토큰이 비어있습니다. userId={}", event.userId());
            return;
        }
        
        User user = userQueryUseCase.findById(event.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        fcmTokenRepository.save(FcmToken.create(user, event.fcmToken()));
    }

    /**
     * <h3>사용자 로그아웃 이벤트 처리</h3>
     * <p>사용자 로그아웃 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param event 사용자 로그아웃 이벤트
     */
    @EventListener
    @Transactional
    public void handleUserLoggedOutEvent(UserLoggedOutEvent event) {
        log.info("사용자 로그아웃 이벤트 처리: userId={}", event.userId());
        fcmTokenRepository.deleteByUser_Id(event.userId());
    }

    /**
     * <h3>사용자 탈퇴 이벤트 처리</h3>
     * <p>사용자 탈퇴 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param event 사용자 탈퇴 이벤트
     */
    @EventListener
    @Transactional
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        log.info("사용자 탈퇴 이벤트 처리: userId={}", event.userId());
        fcmTokenRepository.deleteByUser_Id(event.userId());
    }
}
