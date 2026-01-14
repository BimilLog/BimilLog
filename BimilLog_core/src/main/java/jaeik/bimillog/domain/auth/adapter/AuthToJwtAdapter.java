package jaeik.bimillog.domain.auth.adapter;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.web.JwtFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthToJwtAdapter {
    private final JwtFactory jwtFactory;

    /**
     * <h3>JWT 액세스 토큰 생성</h3>
     *
     * <p>사용자 정보를 포함한 JWT 액세스 토큰을 생성한다. 유효기간은 1시간이다</p>
     *
     * @param userDetails 사용자 상세 정보
     * @return JWT 액세스 토큰
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        return jwtFactory.generateAccessToken(userDetails);
    }

    /**
     * <h3>JWT 리프레시 토큰 생성</h3>
     *
     * <p>사용자 ID와 토큰 ID를 포함한 JWT 리프레시 토큰을 생성한다. 유효기간은 30일이다.</p>
     *
     * @param userDetails 사용자 상세 정보
     * @return JWT 리프레시 토큰
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        return jwtFactory.generateRefreshToken(userDetails);
    }

    /**
     * <h3>JWT 토큰 해시값 생성</h3>
     * <p>JWT 토큰을 SHA-256 알고리즘으로 해시하여 블랙리스트 키로 사용할 해시값을 생성합니다.</p>
     *
     * @param token 해시할 JWT 토큰 문자열
     * @return SHA-256 해시값 (16진수 문자열)
     */
    public String generateTokenHash(String token) {
        return jwtFactory.generateTokenHash(token);
    }
}
