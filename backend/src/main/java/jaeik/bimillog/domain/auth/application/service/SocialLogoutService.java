package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.infrastructure.adapter.in.auth.web.AuthCommandController;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * <h2>로그아웃 서비스</h2>
 * <p>사용자의 로그아웃 처리와 소셜 플랫폼 연동 해제를 담당하는 서비스입니다.</p>
 * <p>JWT 토큰 무효화, 소셜 플랫폼 로그아웃, 이벤트 발행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLogoutService implements SocialLogoutUseCase {

    private final SocialStrategyRegistryPort strategyRegistry;
    private final GlobalTokenQueryPort globalTokenQueryPort;
    private final GlobalCookiePort globalCookiePort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>사용자 로그아웃 처리</h3>
     * <p>인증된 사용자를 시스템에서 로그아웃 처리합니다.</p>
     * <p>소셜 플랫폼 로그아웃, 보안 컨텍스트 정리, 토큰 무효화 이벤트 발행을 순차적으로 실행합니다.</p>
     * <p>{@link AuthCommandController}에서 POST /api/auth/logout 요청 처리 시 호출됩니다.</p>
     *
     * @param userDetails 현재 인증된 사용자 정보
     * @return ResponseCookie 로그아웃용 쿠키 설정 목록 (토큰 삭제용)
     * @throws AuthCustomException 로그아웃 처리 중 오류 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            // 소셜 로그아웃 처리
            performSocialLogout(userDetails);
            // 로그아웃 이벤트 발행 - 토큰 삭제는 TokenCleanupListener에서 처리
            eventPublisher.publishEvent(UserLoggedOutEvent.of(userDetails.getUserId(), userDetails.getTokenId()));
            SecurityContextHolder.clearContext();
            return globalCookiePort.getLogoutCookies();
        } catch (Exception e) {
            throw new AuthCustomException(AuthErrorCode.LOGOUT_FAIL, e);
        }
    }

    /**
     * <h3>소셜 플랫폼 로그아웃 처리</h3>
     * <p>연동된 소셜 플랫폼에서 사용자를 로그아웃 처리합니다.</p>
     * <p>사용자 토큰 정보를 조회하여 해당 소셜 플랫폼의 로그아웃 API를 호출합니다.</p>
     * <p>소셜 로그아웃 실패 시에도 전체 로그아웃 프로세스를 중단하지 않고 로그만 기록합니다.</p>
     * <p>{@link #logout} 메서드에서 메인 로그아웃 플로우의 일부로 호출됩니다.</p>
     *
     * @param userDetails 현재 인증된 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private void performSocialLogout(CustomUserDetails userDetails) {
        try {
            Optional<Token> optionalToken = globalTokenQueryPort.findById(userDetails.getTokenId());
            if (optionalToken.isPresent()) {
                Token token = optionalToken.get();
                if (token.getUsers() != null) {
                    try {
                        strategyRegistry.getStrategy(token.getUsers().getProvider())
                                .logout(token.getUsers().getProvider(), token.getAccessToken());
                        log.debug("소셜 로그아웃 성공 - 사용자 ID: {}, 제공자: {}",
                                userDetails.getUserId(), token.getUsers().getProvider());
                    } catch (Exception socialLogoutException) {
                        // 소셜 로그아웃 실패는 전체 프로세스를 방해하지 않도록 로그만 기록
                        log.warn("소셜 로그아웃 실패 - 사용자 ID: {}, 제공자: {}, 오류: {}",
                                userDetails.getUserId(),
                                token.getUsers().getProvider(),
                                socialLogoutException.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // 토큰 조회 실패는 전체 프로세스를 방해하지 않도록 로그만 기록
            log.warn("토큰 조회 실패 - 사용자 ID: {}, 토큰 ID: {}, 오류: {}",
                    userDetails.getUserId(), userDetails.getTokenId(), e.getMessage());
        }
    }

}
