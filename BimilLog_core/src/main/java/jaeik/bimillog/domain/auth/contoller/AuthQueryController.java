package jaeik.bimillog.domain.auth.contoller;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.auth.dto.MemberInfoResponseDTO;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>인증 조회 컨트롤러</h2>
 * <p>사용자 정보 조회 등 인증 관련 조회 요청을 처리합니다.</p>
 * <p>현재 로그인 사용자 정보 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthQueryController {

    /**
     * <h3>현재 로그인 사용자 정보 조회</h3>
     * <p>현재 로그인한 사용자의 정보를 조회하여 반환합니다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 3.0.0
     */
    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponseDTO> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.AUTH_NULL_SECURITY_CONTEXT);
        }

        MemberInfoResponseDTO response = MemberInfoResponseDTO.from(userDetails);
        return ResponseEntity.ok(response);
    }
}