package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageNotificationPort;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.repository.notification.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>알림 어댑터</h2>
 * <p>SSE 연결 및 쿠키 관리를 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements ManageNotificationPort {

    private final EmitterRepository emitterRepository;
    private final AuthCookieManager authCookieManager;

    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        emitterRepository.deleteAllEmitterByUserId(userId);
    }

    @Override
    public List<ResponseCookie> getLogoutCookies() {
        return authCookieManager.getLogoutCookies();
    }
}