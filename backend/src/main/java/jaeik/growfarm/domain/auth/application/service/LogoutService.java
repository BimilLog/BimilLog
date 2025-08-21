package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.out.LoadTokenPort;
import jaeik.growfarm.domain.auth.application.port.out.DeleteUserPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>로그아웃 서비스</h2>
 * <p>로그아웃 관련 기능을 처리하는 전용 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final DeleteUserPort deleteUserPort;
    private final SocialLoginPort socialLoginPort;
    private final LoadTokenPort loadTokenPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>로그아웃 처리</h3>
     * <p>사용자를 로그아웃하고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            logoutSocial(userDetails);
            eventPublisher.publishEvent(UserLoggedOutEvent.of(userDetails.getUserId(), userDetails.getTokenId()));
            SecurityContextHolder.clearContext();
            return deleteUserPort.getLogoutCookies();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGOUT_FAIL, e);
        }
    }

    /**
     * <h3>소셜 로그아웃 처리</h3>
     * <p>사용자의 소셜 로그아웃을 수행합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Transactional(readOnly = true)
    public void logoutSocial(CustomUserDetails userDetails) {
        loadTokenPort.findById(userDetails.getTokenId()).ifPresent(token -> {
            User user = token.getUsers();
            if (user != null) {
                socialLoginPort.logout(user.getProvider(), token.getAccessToken());
            }
        });
    }
}
