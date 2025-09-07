package jaeik.bimillog.infrastructure.adapter.auth.in.web;

import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.infrastructure.adapter.auth.dto.UserInfoResponseDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>인증 조회 컨트롤러</h2>
 * <p>사용자 정보 조회, 서버 상태 조회 등 인증 관련 조회 요청을 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthQueryController {

    /**
     * <h3>현재 로그인한 사용자 정보 조회 API</h3>
     * <p>현재 로그인한 사용자의 정보를 조회하여 반환</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 현재 로그인한 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDTO> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthCustomException(AuthErrorCode.NULL_SECURITY_CONTEXT);
        }

        UserInfoResponseDTO response = UserInfoResponseDTO.from(userDetails.getUserDetail());
        return ResponseEntity.ok(response);
    }

    /**
     * <h3>서버 상태 검사 API</h3>
     *
     * @return 상태 검사 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}