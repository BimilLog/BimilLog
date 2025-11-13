package jaeik.bimillog.domain.global.strategy;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.entity.SocialProvider;

/**
 * <h2>소셜 인증 전략</h2>
 * <p>플랫폼별 OAuth 인증 흐름을 캡슐화합니다.</p>
 */
public interface SocialAuthStrategy {

    /**
     * 지원하는 소셜 제공자를 반환합니다.
     *
     * @return 소셜 제공자
     */
    SocialProvider getProvider();

    /**
     * OAuth 인증 코드를 사용해 소셜 토큰 및 사용자 프로필을 조회합니다.
     *
     * @param code OAuth 2.0 인증 코드
     * @param state OAuth 2.0 state 파라미터 (CSRF 방지용, 일부 제공자에서 필수)
     * @return 소셜 토큰과 사용자 정보를 포함한 프로필
     */
    SocialMemberProfile getSocialToken(String code, String state);

    /**
     * 플랫폼과의 연결을 해제합니다.
     *
     * @param socialId 소셜 플랫폼 사용자 식별자
     * @param accessToken 소셜 플랫폼 액세스 토큰 (네이버 등 토큰 삭제 API 호출용)
     */
    void unlink(String socialId, String accessToken);

    /**
     * 플랫폼 세션을 로그아웃 처리합니다.
     *
     * @param accessToken 소셜 플랫폼 액세스 토큰
     * @throws Exception 로그아웃 처리 중 예외 발생 시
     */
    void logout(String accessToken) throws Exception;

    /**
     * 강제 로그아웃
     * @param socialId 소셜 플랫폼 사용자 식별자
     */
    void forceLogout(String socialId);

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신
     *
     * @param refreshToken 소셜 플랫폼 리프레시 토큰
     * @return 갱신된 액세스 토큰
     * @throws Exception 토큰 갱신 실패 시
     */
    String refreshAccessToken(String refreshToken) throws Exception;
}
