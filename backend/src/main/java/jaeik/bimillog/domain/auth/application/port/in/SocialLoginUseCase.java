
package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>소셜 로그인 유스케이스</h2>
 * <p>소셜 로그인을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginUseCase {

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


}
