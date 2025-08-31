package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.infrastructure.auth.CustomUserDetails;

/**
 * <h2>소셜 로그아웃 포트</h2>
 * <p>소셜 로그아웃 처리를 위한 전용 포트</p>
 * <p>LogoutService와 WithdrawService 간의 중복된 소셜 로그아웃 로직을 공통화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface SocialLogoutPort {

    /**
     * <h3>사용자의 소셜 로그아웃 처리</h3>
     * <p>사용자의 토큰을 조회하여 해당 소셜 플랫폼에서 로그아웃을 수행합니다.</p>
     * <p>토큰이 존재하지 않거나 사용자 정보가 없는 경우 조용히 무시됩니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    void performSocialLogout(CustomUserDetails userDetails);
}