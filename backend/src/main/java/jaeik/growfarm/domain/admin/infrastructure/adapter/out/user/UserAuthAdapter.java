package jaeik.growfarm.domain.admin.infrastructure.adapter.out.user;

import jaeik.growfarm.domain.admin.application.port.out.UserAuthPort;
import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageNotificationPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>관리자 사용자 인증 어댑터</h2>
 * <p>관리자 권한으로 사용자 인증 관련 작업을 처리하기 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserAuthAdapter implements UserAuthPort {

    private final LoadUserPort loadUserPort;
    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageNotificationPort manageNotificationPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void forceWithdraw(Long userId) {
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 소셜 로그인 연동 해제
        socialLoginPort.unlink(user.getProvider(), user.getSocialId());
        
        // 2. 인증 데이터 삭제 (토큰, FCM 토큰, 사용자 데이터)
        manageAuthDataPort.performWithdrawProcess(userId);
        
        // 3. SSE 연결 정리
        manageNotificationPort.deleteAllEmitterByUserId(userId);

        // 4. 탈퇴 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
    }
}
