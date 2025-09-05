package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.LogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialLogoutPort;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    private final SocialLogoutPort socialLogoutPort;
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
            // 공통화된 소셜 로그아웃 포트 사용
            socialLogoutPort.performSocialLogout(userDetails);
            // 로그아웃 이벤트 발행 - 토큰 삭제는 TokenCleanupListener에서 처리
            eventPublisher.publishEvent(UserLoggedOutEvent.of(userDetails.getUserId(), userDetails.getTokenId()));
            SecurityContextHolder.clearContext();
            return deleteUserPort.getLogoutCookies();
        } catch (Exception e) {
            throw new AuthCustomException(AuthErrorCode.LOGOUT_FAIL, e);
        }
    }
}
