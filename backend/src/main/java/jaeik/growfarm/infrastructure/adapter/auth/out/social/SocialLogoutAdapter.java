package jaeik.growfarm.infrastructure.adapter.auth.out.social;

import jaeik.growfarm.domain.auth.application.port.out.LoadTokenPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLogoutPort;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>소셜 로그아웃 어댑터</h2>
 * <p>소셜 로그아웃 처리를 위한 어댑터 클래스</p>
 * <p>LogoutService와 WithdrawService에서 공통으로 사용되는 소셜 로그아웃 로직을 분리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLogoutAdapter implements SocialLogoutPort {

    private final SocialLoginPort socialLoginPort;
    private final LoadTokenPort loadTokenPort;

    /**
     * <h3>소셜 로그아웃 수행</h3>
     * <p>사용자의 토큰 정보를 조회하여 해당 소셜 플랫폼에서 로그아웃을 처리합니다.</p>
     * <p>예외가 발생하더라도 전체 프로세스를 방해하지 않도록 조용히 처리됩니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional(readOnly = true)
    public void performSocialLogout(CustomUserDetails userDetails) {
        try {
            loadTokenPort.findById(userDetails.getTokenId()).ifPresent(token -> {
                User user = token.getUsers();
                if (user != null) {
                    socialLoginPort.logout(user.getProvider(), token.getAccessToken());
                }
            });
        } catch (Exception e) {
            // 소셜 로그아웃 실패는 전체 프로세스를 방해하지 않도록 로그만 기록
            log.warn("소셜 로그아웃 실패 (사용자 ID: {}): {}", userDetails.getUserId(), e.getMessage());
        }
    }
}