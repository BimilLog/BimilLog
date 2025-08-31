package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.TokenVO;

/**
 * <h2>소셜 로그인 포트</h2>
 * <p>소셜 로그인 처리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginPort {

    /**
     * <h3>소셜 사용자 프로필</h3>
     * <p>소셜 로그인으로부터 받은 사용자 프로필 정보를 담는 순수 도메인 모델</p>
     *
     * @param socialId 소셜 ID
     * @param email 이메일 주소
     * @param provider 소셜 제공자
     * @param nickname 사용자 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @since 2.0.0
     * @author Jaeik
     */
    record SocialUserProfile(String socialId, String email, SocialProvider provider, 
                           String nickname, String profileImageUrl) {}

    /**
     * <h3>소셜 로그인 결과</h3>
     * <p>소셜 로그인 처리 결과를 담는 레코드 클래스</p>
     *
     * @param userProfile 소셜 사용자 프로필
     * @param token 토큰 정보
     * @param isNewUser 신규 사용자 여부
     * @since 2.0.0
     * @author Jaeik
     */
    record LoginResult(SocialUserProfile userProfile, TokenVO token, boolean isNewUser) {}

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