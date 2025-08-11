
package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResponseDTO;

public interface SocialLoginUseCase {
    LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken);
}
