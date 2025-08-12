package jaeik.growfarm.dto.auth;

import jaeik.growfarm.dto.user.TokenDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>로그인 결과 DTO</h2>
 * <p>소셜 로그인 후 사용자 정보 및 토큰, 로그인 타입을 포함하는 DTO</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResultDTO {

    private SocialLoginUserData userData;
    private TokenDTO tokenDTO;
    private LoginType loginType; // 로그인 유형 추가

    public enum LoginType {
        NEW_USER, EXISTING_USER
    }
}
