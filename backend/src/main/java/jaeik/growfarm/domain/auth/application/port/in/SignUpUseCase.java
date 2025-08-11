
package jaeik.growfarm.domain.auth.application.port.in;

import org.springframework.http.ResponseCookie;

import java.util.List;

public interface SignUpUseCase {
    List<ResponseCookie> signUp(String userName, String uuid);
}
