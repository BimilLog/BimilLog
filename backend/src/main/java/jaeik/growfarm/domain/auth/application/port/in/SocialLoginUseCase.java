
package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResponseDTO;

/**
 * <h2>소셜 로그인 유스케이스</h2>
 * <p>소셜 로그인을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginUseCase {
    LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken);
}
