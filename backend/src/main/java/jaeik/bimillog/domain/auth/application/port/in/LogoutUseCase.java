
package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>로그아웃 유스케이스</h2>
 * <p>
 * 사용자의 로그아웃 요청을 처리하는 비즈니스 로직의 진입점입니다.
 * </p>
 * <p>AuthController에서 사용자 로그아웃 요청 시 호출되어 세션 무효화와 쿠키 제거를 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LogoutUseCase {

    /**
     * <h3>로그아웃 처리</h3>
     * <p>사용자의 로그아웃 요청을 처리하여 세션을 무효화하고 쿠키를 제거합니다.</p>
     * <p>소셜 로그인 세션도 함께 해제하여 완전한 로그아웃을 보장합니다.</p>
     * <p>AuthController에서 POST /api/auth/logout 요청 처리 시 호출됩니다.</p>
     *
     * @param userDetails 인증된 사용자의 세부 정보
     * @return 로그아웃 처리를 위한 쿠키 제거 지시가 담긴 ResponseCookie 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> logout(CustomUserDetails userDetails);
}
