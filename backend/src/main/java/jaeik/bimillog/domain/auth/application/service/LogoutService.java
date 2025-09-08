package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.LogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.auth.application.port.out.LoadTokenPort;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final DeleteUserPort deleteUserPort;
    private final SocialPort socialPort;
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
            // 소셜 로그아웃 처리
            performSocialLogout(userDetails);
            // 로그아웃 이벤트 발행 - 토큰 삭제는 TokenCleanupListener에서 처리
            eventPublisher.publishEvent(UserLoggedOutEvent.of(userDetails.getUserId(), userDetails.getTokenId()));
            SecurityContextHolder.clearContext();
            return deleteUserPort.getLogoutCookies();
        } catch (Exception e) {
            throw new AuthCustomException(AuthErrorCode.LOGOUT_FAIL, e);
        }
    }

    /**
     * <h3>소셜 로그아웃 수행</h3>
     * <p>사용자의 토큰 정보를 조회하여 해당 소셜 플랫폼에서 로그아웃을 처리합니다.</p>
     * <p>예외가 발생하더라도 전체 프로세스를 방해하지 않도록 조용히 처리됩니다.</p>
     * <p>트랜잭션 경계를 명시적으로 설정하여 데이터 일관성을 보장합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Transactional(readOnly = true)
    private void performSocialLogout(CustomUserDetails userDetails) {
        try {
            loadTokenPort.findById(userDetails.getTokenId()).ifPresent(token -> {
                if (token.getUsers() != null) {
                    try {
                        socialPort.logout(token.getUsers().getProvider(), token.getAccessToken());
                        log.debug("소셜 로그아웃 성공 - 사용자 ID: {}, 제공자: {}", 
                                userDetails.getUserId(), token.getUsers().getProvider());
                    } catch (Exception socialLogoutException) {
                        // 소셜 로그아웃 실패는 전체 프로세스를 방해하지 않도록 로그만 기록
                        log.warn("소셜 로그아웃 실패 - 사용자 ID: {}, 제공자: {}, 오류: {}", 
                                userDetails.getUserId(), token.getUsers().getProvider(), socialLogoutException.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            // 토큰 조회 실패는 전체 프로세스를 방해하지 않도록 로그만 기록
            log.warn("토큰 조회 실패 - 사용자 ID: {}, 토큰 ID: {}, 오류: {}", 
                    userDetails.getUserId(), userDetails.getTokenId(), e.getMessage());
        }
    }
}
