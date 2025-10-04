
package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.member.entity.SocialProvider;

/**
 * <h2>소셜 로그아웃 유스케이스</h2>
 * <p>사용자의 로그아웃 요청을 처리하는 유스케이스입니다.</p>
 * <p>세션 무효화, 쿠키 제거, 소셜 플랫폼 로그아웃 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLogoutUseCase {

    /**
     * <h3>로그아웃 처리</h3>
     * <p>소셜 로그아웃</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    void socialLogout(Long memberId, SocialProvider provider, Long authTokenId) throws Exception;

    /**
     * <h3>강제 로그아웃 처리</h3>

     * @author Jaeik
     * @since 2.0.0
     */
    void forceLogout(Long memberId, SocialProvider provider, String socialId);
}
