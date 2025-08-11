package jaeik.growfarm.domain.auth.infrastructure.adapter.in;

import jaeik.growfarm.domain.auth.application.port.in.AuthLoginUseCase;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseCookie;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthCommandController {

    private final AuthLoginUseCase authLoginUseCase;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/login")
    public ResponseEntity<?> socialLogin(@RequestParam String provider, @RequestParam String code,
                                                      @RequestParam(required = false) String fcmToken) {
        LoginResponseDTO<?> loginResponse = authLoginUseCase.processSocialLogin(SocialProvider.valueOf(provider.toUpperCase()), code, fcmToken);

        if (loginResponse.getType() == LoginResponseDTO.LoginType.NEW_USER) {
            Map<String, String> uuidData = new HashMap<>();
            String uuid = (String) loginResponse.getData();
            uuidData.put("uuid", uuid);
            return ResponseEntity.ok().header("Set-Cookie", authCookieManager.createTempCookie(uuid).toString()).body(uuidData);
        }

        List<ResponseCookie> cookies = (List<ResponseCookie>) loginResponse.getData();
        return ResponseEntity.ok().headers(headers -> cookies.forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestParam String userName, @CookieValue String uuid) {
        return ResponseEntity.ok().headers(headers -> authLoginUseCase.signUp(userName, uuid).forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().headers(headers -> authLoginUseCase.logout(userDetails).forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().headers(headers -> authLoginUseCase.withdraw(userDetails).forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }
}