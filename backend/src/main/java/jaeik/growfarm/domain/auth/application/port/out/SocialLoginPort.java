package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;

/**
 * <h2>소셜 로그인 포트</h2>
 * <p>소셜 로그인 처리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginPort {

    /**
     * <h3>소셜 로그인 결과</h3>
     * <p>소셜 로그인 처리 결과를 담는 레코드 클래스</p>
     *
     * @param userData 소셜 사용자 데이터
     * @param token 토큰 정보
     * @param isNewUser 신규 사용자 여부
     * @since 2.0.0
     * @author Jaeik
     */
    record LoginResult(SocialLoginUserData userData, TokenDTO token, boolean isNewUser) {}

    /**
     * <h3>소셜 로그인</h3>
     *
     * @param provider 소셜 제공자
     * @param code     인가 코드
     * @return 로그인 결과
     * @since 2.0.0
     * @author Jaeik
     */
    LoginResult login(SocialProvider provider, String code);

    /**
     * <h3>소셜 연결 해제</h3>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void unlink(SocialProvider provider, String socialId);

    /**
     * <h3>소셜 로그아웃</h3>
     *
     * @param provider    소셜 제공자
     * @param accessToken 액세스 토큰
     * @since 2.0.0
     * @author Jaeik
     */
    void logout(SocialProvider provider, String accessToken);
}