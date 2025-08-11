
package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.global.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

public interface LogoutUseCase {
    List<ResponseCookie> logout(CustomUserDetails userDetails);
}
