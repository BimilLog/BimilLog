
package jaeik.growfarm.domain.auth.application.port.in;

import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>소셜 로그인 유스케이스</h2>
 * <p>소셜 로그인을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SocialLoginUseCase {

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 요청을 처리하고 로그인 결과를 반환합니다.</p>
     *
     * @param provider  소셜 제공자
     * @param code      인가 코드
     * @param fcmToken  Firebase Cloud Messaging 토큰
     * @return 로그인 응답 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken);

    /**
     * <h3>신규 사용자 등록 처리</h3>
     * <p>임시 데이터를 기반으로 신규 사용자를 등록하고 로그인 쿠키를 생성합니다.</p>
     *
     * @param userName 사용자 닉네임
     * @param uuid     임시 데이터 UUID
     * @return 로그인 응답 DTO (신규 사용자 등록)
     * @since 2.1.0
     * @author Jaeik
     */
    LoginResponseDTO<List<ResponseCookie>> registerNewUser(String userName, String uuid); // 반환 타입 변경
}
