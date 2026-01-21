package jaeik.bimillog.domain.member.adapter;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.web.JwtFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberToJwtAdapter {
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
}
