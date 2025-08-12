package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.AuthPort;
import jaeik.growfarm.global.auth.AuthCookieManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>인증 어댑터</h2>
 * <p>인증과 관련된 쿠키 관리를 위한 어댑터</p>
 * <p>AuthCookieManager를 통해 실제 쿠키 관리 작업을 위임</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthAdapter implements AuthPort {

    private final AuthCookieManager authCookieManager;

    @Override
    public List<ResponseCookie> getLogoutCookies() {
        return authCookieManager.getLogoutCookies();
    }

    @Override
    public ResponseCookie createTempCookie(String uuid) {
        return authCookieManager.createTempCookie(uuid);
    }
}