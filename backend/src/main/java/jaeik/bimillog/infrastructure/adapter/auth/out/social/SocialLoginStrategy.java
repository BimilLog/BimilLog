package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;

/**
 * <h2>소셜 로그인 전략 인터페이스</h2>
 * <p>Strategy 패턴을 적용한 소셜 로그인 전략의 공통 인터페이스입니다.</p>
 * <p>OAuth 2.0 인증 플로우, 사용자 정보 조회, 계정 연결 해제, 로그아웃</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginStrategy {

    /**
     * <h3>전략별 로그인 결과 레코드</h3>
     * <p>소셜 로그인 처리 결과를 전달하는 레코드 클래스입니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (도메인 모델)
     * @param token 소셜 로그인에서 획득한 토큰 정보 (도메인 모델)
     * @author Jaeik
     * @since 2.0.0
     */
    record StrategyLoginResult(LoginResult.SocialUserProfile userProfile, Token token) {}

    /**
     * <h3>소셜 로그인 인증 처리</h3>
     * <p>OAuth 2.0 인증 코드를 사용하여 소셜 로그인 플로우를 처리합니다.</p>
     *
     * @param code 소셜 로그인 OAuth 2.0 인증 코드
     * @return StrategyLoginResult 소셜 로그인 결과
     * @author Jaeik
     * @since 2.0.0
     */
    StrategyLoginResult login(String code);

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>특정 사용자의 소셜 계정 연결을 완전히 해제합니다.</p>
     *
     * @param socialId 연결 해제할 소셜 사용자 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    void unlink(String socialId);

    /**
     * <h3>소셜 로그아웃 처리</h3>
     * <p>사용자의 소셜 계정 세션을 종료시킵니다.</p>
     *
     * @param accessToken 소셜 로그인 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    void logout(String accessToken);

    /**
     * <h3>소셜 로그인 제공자 식별자 반환</h3>
     * <p>현재 Strategy 구현체가 처리하는 소셜 로그인 제공자 타입을 반환합니다.</p>
     *
     * @return SocialProvider 소셜 로그인 제공자 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    SocialProvider getProvider();
}
