package jaeik.bimillog.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>로그인 결과 값 객체</h2>
 * <p>소셜 로그인 처리 결과를 담는 도메인 계층의 값 객체입니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO {
    private String jwtAccessToken;
    private String jwtRefreshToken;

    public static LoginResultDTO createLoginResult(String jwtAccessToken, String jwtRefreshToken) {
        return new LoginResultDTO(jwtAccessToken, jwtRefreshToken);
    }
}
