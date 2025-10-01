
package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.in.auth.web.AuthCommandController;

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
     * <p>사용자의 로그아웃 요청을 처리하여 세션을 무효화하고 쿠키를 제거합니다.</p>
     * <p>소셜 로그인 세션도 함께 해제하여 완전한 로그아웃을 보장합니다.</p>
     * <p>{@link AuthCommandController}에서 POST /api/auth/logout 요청 처리 시 호출됩니다.</p>
     *
     * @param userDetails 인증된 사용자의 세부 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void logout(Long memberId, SocialProvider provider, Long tokenId);

    /**
     * <h3>강제 로그아웃 처리</h3>

     * @author Jaeik
     * @since 2.0.0
     */
    void forceLogout(Long memberId, SocialProvider provider, String socialId);
}
