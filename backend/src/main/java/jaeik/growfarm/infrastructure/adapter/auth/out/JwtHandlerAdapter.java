package jaeik.growfarm.infrastructure.adapter.auth.out;

import jaeik.growfarm.domain.auth.application.port.out.AuthPort;
import jaeik.growfarm.infrastructure.auth.JwtHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * <h2>JWT 핸들러 어댑터</h2>
 * <p>JwtHandler를 AuthPort 인터페이스로 래핑하는 어댑터</p>
 * <p>헥사고날 아키텍처의 아웃바운드 어댑터 역할</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtHandlerAdapter implements AuthPort {

    private final JwtHandler jwtHandler;

    /**
     * <h3>토큰 만료 시간 조회</h3>
     * <p>JWT 토큰에서 만료 시간을 추출합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 만료 시간
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Date getTokenExpiration(String token) {
        return jwtHandler.getTokenExpiration(token);
    }

    /**
     * <h3>토큰 유효성 검증</h3>
     * <p>JWT 토큰의 유효성을 검증합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 유효성 여부
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean validateToken(String token) {
        return jwtHandler.validateToken(token);
    }

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>JWT 토큰을 해시화하여 블랙리스트 저장용 키를 생성합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 해시값
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String generateTokenHash(String token) {
        return jwtHandler.generateTokenHash(token);
    }
}