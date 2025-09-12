package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;

/**
 * <h2>소셜 로그인 전략 포트</h2>
 * <p>소셜 플랫폼별 OAuth 인증 전략을 도메인 레벨에서 관리하는 포트입니다.</p>
 * <p>OAuth 인증, 토큰 교환, 사용자 프로필 조회 등 순수 인증 로직</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginStrategyPort {

    /**
     * <h3>전략별 로그인 결과 레코드</h3>
     * <p>소셜 로그인 인증 결과를 전달하는 레코드 클래스입니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (도메인 모델)
     * @param token 소셜 로그인에서 획득한 토큰 정보 (도메인 모델)
     * @author Jaeik
     * @since 2.0.0
     */
    record StrategyLoginResult(SocialAuthData.SocialUserProfile userProfile, Token token) {}

    /**
     * <h3>소셜 플랫폼 OAuth 인증</h3>
     * <p>제공자별 전략을 사용하여 OAuth 2.0 인증 플로우를 처리합니다.</p>
     * <p>인증 코드를 받아 액세스 토큰을 교환하고 사용자 프로필을 조회합니다.</p>
     * <p>{@link SocialService}에서 소셜 로그인 인증 단계 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO 등)
     * @param code 소셜 플랫폼에서 발급한 OAuth 2.0 인증 코드
     * @return 인증 결과 (사용자 프로필과 토큰 정보 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    StrategyLoginResult authenticate(SocialProvider provider, String code);

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>제공자별 전략을 사용하여 소셜 플랫폼과의 연결을 해제합니다.</p>
     * <p>각 플랫폼의 관리자 API를 통해 앱 연동을 완전히 차단합니다.</p>
     * <p>{@link SocialService}에서 회원 탈퇴 또는 계정 차단 시 호출됩니다.</p>
     *
     * @param provider 연결을 해제할 소셜 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void unlink(SocialProvider provider, String socialId);

    /**
     * <h3>소셜 플랫폼 로그아웃</h3>
     * <p>제공자별 전략을 사용하여 소셜 플랫폼에서 로그아웃 처리를 수행합니다.</p>
     * <p>사용자의 소셜 플랫폼 세션을 종료하여 완전한 로그아웃을 보장합니다.</p>
     * <p>{@link SocialService}에서 사용자 로그아웃 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 로그아웃할 소셜 제공자 (KAKAO 등)
     * @param accessToken 소셜 플랫폼의 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    void logout(SocialProvider provider, String accessToken);
}