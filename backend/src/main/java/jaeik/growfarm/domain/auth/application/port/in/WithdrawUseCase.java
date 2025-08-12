
package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

public interface WithdrawUseCase {
    List<ResponseCookie> withdraw(CustomUserDetails userDetails);
}
