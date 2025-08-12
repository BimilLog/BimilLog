
package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>로그아웃 유스케이스</h2>
 * <p>사용자 로그아웃을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LogoutUseCase {

    /**
     * <h3>로그아웃 처리</h3>
     * <p>사용자 로그아웃 요청을 처리하고 쿠키를 반환합니다.</p>
     *
     * @param userDetails 사용자 세부 정보
     * @return 쿠키 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> logout(CustomUserDetails userDetails);
}
