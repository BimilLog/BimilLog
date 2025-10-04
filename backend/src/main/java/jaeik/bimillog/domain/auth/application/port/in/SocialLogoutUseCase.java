
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
     * <h3>소셜 플랫폼 로그아웃</h3>
     * <p>사용자의 소셜 플랫폼 세션을 로그아웃 처리합니다.</p>
     * <p>카카오 등 소셜 플랫폼의 API를 호출하여 사용자의 세션을 종료합니다.</p>
     *
     * @param memberId 회원 ID
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param authTokenId 인증 토큰 ID (현재 사용되지 않음)
     * @throws Exception 소셜 플랫폼 로그아웃 처리 중 예외 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    void socialLogout(Long memberId, SocialProvider provider, Long authTokenId) throws Exception;

    /**
     * <h3>강제 로그아웃</h3>
     * <p>관리자에 의한 강제 로그아웃 처리입니다.</p>
     * <p>사용자 차단 등의 관리 작업 시 소셜 세션을 강제로 종료합니다.</p>
     * <p>현재 미구현 상태입니다.</p>
     *
     * @param memberId 회원 ID
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void forceLogout(Long memberId, SocialProvider provider, String socialId);
}
