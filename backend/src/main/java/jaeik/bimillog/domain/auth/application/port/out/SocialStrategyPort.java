package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.KakaoUserInfo;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;

/**
 * <h2>소셜 로그인 전략 포트</h2>
 * <p>소셜 플랫폼별 OAuth 2.0 인증 전략을 도메인 레벨에서 관리하는 포트입니다.</p>
 * <p>OAuth 인증 코드 처리, 토큰 교환, 사용자 프로필 조회, 계정 연동 해제 등 인증 로직</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialStrategyPort {

    /**
     * <h3>지원하는 소셜 제공자 반환</h3>
     * <p>해당 전략이 지원하는 소셜 제공자를 반환합니다.</p>
     * <p>레지스트리에서 전략 등록 시 자동 매핑을 위해 사용됩니다.</p>
     *
     * @return 지원하는 소셜 제공자 (KAKAO, GOOGLE, NAVER 등)
     * @author Jaeik
     * @since 2.0.0
     */
    SocialProvider getSupportedProvider();

    /**
     * <h3>소셜 플랫폼 OAuth 토큰 발급</h3>
     * <p>OAuth 2.0 인증 코드를 사용하여 소셜 플랫폼으로부터 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     * <p>각 플랫폼의 토큰 엔드포인트에 인증 코드를 전송하고 토큰을 받아옵니다.</p>
     *
     * @param code OAuth 2.0 인증 코드
     * @return KakaoToken 액세스 토큰과 리프레시 토큰을 담은 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    KakaoToken getToken(String code);

    /**
     * <h3>소셜 플랫폼 사용자 정보 조회</h3>
     * <p>발급받은 액세스 토큰을 사용하여 소셜 플랫폼의 사용자 정보 API에서 프로필 데이터를 조회합니다.</p>
     * <p>각 플랫폼의 API 응답을 DTO로 변환하여 반환합니다.</p>
     *
     * @param accessToken 소셜 플랫폼 액세스 토큰
     * @param refreshToken 소셜 플랫폼 리프레시 토큰
     * @return KakaoUserInfo 소셜 사용자 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    KakaoUserInfo getUserInfo(String accessToken);

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>제공자별 전략을 사용하여 소셜 플랫폼과의 연결을 해제합니다.</p>
     * <p>각 플랫폼의 관리자 API를 통해 앱 연동을 완전히 차단합니다.</p>
     * <p>{@link SocialLoginService}에서 회원 탈퇴 또는 계정 차단 시 호출됩니다.</p>
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
     * <p>{@link SocialLoginService}에서 사용자 로그아웃 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 로그아웃할 소셜 제공자 (KAKAO 등)
     * @param accessToken 소셜 플랫폼의 액세스 토큰
     * @throws Exception 소셜 플랫폼 API 호출 실패 시 (네트워크 오류, API 서버 오류 등)
     * @author Jaeik
     * @since 2.0.0
     */
    void logout(SocialProvider provider, String accessToken) throws Exception;
}