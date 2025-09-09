
package jaeik.bimillog.domain.auth.application.port.in;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>자체 서비스 회원가입 유스케이스</h2>
 * <p>
 * 소셜 로그인 후 신규 사용자의 회원가입 과정을 완료하는 비즈니스 로직의 진입점입니다.
 * </p>
 * <p>AuthController에서 신규 사용자가 닉네임을 입력하고 회원가입을 완료할 때 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SignUpUseCase {

    /**
     * <h3>회원가입 처리</h3>
     * <p>신규 사용자의 회원가입을 완료하고 인증 쿠키를 생성합니다.</p>
     * <p>Redis에 저장된 임시 소셜 로그인 데이터를 기반으로 실제 사용자 계정을 생성합니다.</p>
     * <p>AuthController에서 POST /api/auth/signup 요청 처리 시 호출됩니다.</p>
     *
     * @param userName 사용자가 입력한 닉네임
     * @param uuid Redis에 저장된 임시 사용자 데이터의 UUID 키
     * @return JWT 토큰이 포함된 인증 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> signUp(String userName, String uuid);
}
