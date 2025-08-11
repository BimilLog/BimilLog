package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResultDTO;

/**
 * <h2>소셜 로그인 포트</h2>
 * <p>소셜 로그인 처리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginPort {

    /**
     * <h3>소셜 로그인</h3>
     *
     * @param provider 소셜 제공자
     * @param code     인가 코드
     * @return 로그인 결과
     */
    LoginResultDTO login(SocialProvider provider, String code);

    /**
     * <h3>소셜 연결 해제</h3>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     */
    void unlink(SocialProvider provider, String socialId);

    /**
     * <h3>소셜 로그아웃</h3>
     *
     * @param provider    소셜 제공자
     * @param accessToken 액세스 토큰
     */
    void logout(SocialProvider provider, String accessToken);
}