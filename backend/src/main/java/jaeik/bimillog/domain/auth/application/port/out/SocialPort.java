package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>소셜 포트</h2>
 * <p>
 * 소셜 플랫폼(카카오 등)과의 외부 연동 작업을 처리하는 포트입니다.
 * </p>
 * <p>소셜 로그인, 로그아웃, 연결 해제 등 외부 소셜 API 호출을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialPort {

    /**
     * <h3>소셜 로그인</h3>
     * <p>소셜 플랫폼의 인가 코드를 사용하여 사용자 정보와 액세스 토큰을 조회합니다.</p>
     * <p>OAuth 2.0 인증 플로우를 통해 사용자 프로필과 토큰 정보를 반환합니다.</p>
     * <p>SocialService에서 소셜 로그인 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param code 소셜 플랫폼에서 발급한 인가 코드
     * @return 소셜 로그인 결과 (사용자 프로필, 액세스 토큰, 기존 회원 여부 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    LoginResult.SocialLoginData login(SocialProvider provider, String code);

    /**
     * <h3>소셜 연결 해제</h3>
     * <p>소셜 플랫폼과 애플리케이션 간의 연결을 해제합니다.</p>
     * <p>사용자 차단이나 회원 탈퇴 시 소셜 플랫폼에서도 연결을 끊어 보안을 강화합니다.</p>
     * <p>SocialService에서 소셜 계정 연결 해제 요청 시 호출됩니다.</p>
     *
     * @param provider 연결을 해제할 소셜 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void unlink(SocialProvider provider, String socialId);

    /**
     * <h3>소셜 로그아웃</h3>
     * <p>소셜 플랫폼에서의 로그아웃 처리를 수행합니다.</p>
     * <p>애플리케이션 로그아웃과 함께 소셜 플랫폼에서도 세션을 종료하여 완전한 로그아웃을 보장합니다.</p>
     * <p>LogoutService에서 사용자 로그아웃 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 로그아웃할 소셜 제공자 (KAKAO 등)
     * @param accessToken 소셜 플랫폼의 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    void logout(SocialProvider provider, String accessToken);
}