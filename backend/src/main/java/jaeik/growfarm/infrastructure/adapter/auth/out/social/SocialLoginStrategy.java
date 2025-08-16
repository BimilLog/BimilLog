package jaeik.growfarm.infrastructure.adapter.auth.out.social;

import jaeik.growfarm.domain.common.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResultDTO;

/**
 * <h2>소셜 로그인 전략 인터페이스</h2>
 * <p>소셜 로그인 관련 작업을 정의하는 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginStrategy {

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 코드를 받아 사용자 정보를 조회하고 로그인 결과를 반환합니다.</p>
     *
     * @param code 소셜 로그인 코드
     * @return 로그인 결과 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    LoginResultDTO login(String code);

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>주어진 소셜 ID에 해당하는 계정의 연결을 해제합니다.</p>
     *
     * @param socialId 소셜 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void unlink(String socialId);

    /**
     * <h3>소셜 로그아웃</h3>
     * <p>사용자의 소셜 로그아웃을 처리합니다.</p>
     *
     * @param accessToken 액세스 토큰
     * @since 2.0.0
     * @author Jaeik
     */
    void logout(String accessToken);

    /**
     * <h3>소셜 제공자 정보 조회</h3>
     * <p>현재 소셜 로그인 전략에 해당하는 소셜 제공자 정보를 반환합니다.</p>
     *
     * @return 소셜 제공자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    SocialProvider getProvider();
}
