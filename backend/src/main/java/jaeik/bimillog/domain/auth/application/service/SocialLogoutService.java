package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.infrastructure.adapter.in.auth.web.AuthCommandController;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

    /**
     * <h3>사용자 로그아웃 처리</h3>
     * <p>인증된 사용자를 시스템에서 로그아웃 처리합니다.</p>
     * <p>로그아웃 프로세스를 오케스트레이션 하는 메서드입니다.</p>
     * <p>소셜 플랫폼 로그아웃, 이벤트 발행, 보안 컨텍스트 정리를 순차적으로 실행합니다.</p>
     * <p>{@link AuthCommandController}에서 POST /api/auth/logout 요청 처리 시 호출됩니다.</p>
     *
     * @param userDetails 현재 인증된 사용자 정보
     * @throws AuthCustomException 토큰을 찾을 수 없는 경우 ({@link AuthErrorCode#NOT_FIND_TOKEN})
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(CustomUserDetails userDetails) {
        Token token = getToken(userDetails);
        performSocialLogout(userDetails, token);
        SecurityContextHolder.clearContext();
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
    private void performSocialLogout(CustomUserDetails userDetails, Token token) {
        try {
            SocialProvider socialProvider = userDetails.getSocialProvider();
            strategyRegistry.getStrategy(socialProvider).logout(socialProvider, token.getAccessToken());
            log.debug("소셜 로그아웃 성공 - 사용자 ID: {}, 제공자: {}", userDetails.getUserId(), socialProvider);
        } catch (Exception e) {
            // 소셜 로그아웃 실패시에도 데이터 처리 계속 진행 로그만 남김
            log.error("소셜 로그아웃 실패 - 사용자 ID: {}, 제공자: {}, 오류: {}",
                    userDetails.getUserId(),
                    userDetails.getSocialProvider(),
                    e.getMessage());
        }
    }

    /**
     * <h3>로그아웃 시 토큰 조회</h3>
     * <p>토큰 ID로 실제 토큰을 조회합니다.</p>
     * <p>토큰 조회 실패시 예외가 발생하며 로그아웃 로직이 종료됩니다.</p>
     * <p>해당 예외 발생시 재로그인이 필요합니다.</p>
     * <p>{@link #logout} 메서드에서 로그아웃 플로우에서 가장 먼저 호출됩니다.</p>
     *
     * @param userDetails 현재 인증된 사용자 정보
     * @return Token 조회된 토큰 엔티티
     * @throws AuthCustomException 토큰을 찾을 수 없는 경우 ({@link AuthErrorCode#NOT_FIND_TOKEN})
     * @author Jaeik
     * @since 2.0.0
     */
    private Token getToken(CustomUserDetails userDetails) {
        // 토큰 조회 실패시 로그아웃 예외 재로그인 필요
        return globalTokenQueryPort.findById(userDetails.getTokenId())
                .orElseThrow(() -> {
                    log.warn("로그아웃 토큰 조회 실패 - 사용자 ID: {}, 토큰 ID: {}",
                            userDetails.getUserId(), userDetails.getTokenId());
                    return new AuthCustomException(AuthErrorCode.NOT_FIND_TOKEN);
                });
    }
}
