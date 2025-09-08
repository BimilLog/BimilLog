
package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>소셜 유스케이스</h2>
 * <p>소셜 관련 작업을 처리하는 유즈케이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialUseCase {

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 요청을 처리하고 로그인 결과를 반환합니다.</p>
     *
     * @param provider  소셜 제공자
     * @param code      인가 코드
     * @param fcmToken  Firebase Cloud Messaging 토큰
     * @return 타입 안전성이 보장된 로그인 결과
     * @since 2.0.0
     * @author Jaeik
     */
    LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken);

    /**
     * <h3>소셜 로그인 연결 해제</h3>
     * <p>사용자 차단 시 소셜 플랫폼과의 연결을 해제합니다.</p>
     * <p>해제 실패 시에도 사용자 차단 프로세스는 계속 진행됩니다.</p>
     *
     * @param provider 소셜 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void unlinkSocialAccount(SocialProvider provider, String socialId);


}
