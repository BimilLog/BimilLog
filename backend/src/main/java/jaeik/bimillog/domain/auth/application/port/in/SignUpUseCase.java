
package jaeik.bimillog.domain.auth.application.port.in;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>회원가입 유스케이스</h2>
 * <p>회원가입을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SignUpUseCase {

    /**
     * <h3>회원가입 처리</h3>
     * <p>회원가입 요청을 처리하고 쿠키를 반환합니다.</p>
     *
     * @param userName 사용자 이름
     * @param uuid     UUID
     * @return 쿠키 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> signUp(String userName, String uuid);
}
