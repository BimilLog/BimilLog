package jaeik.growfarm.domain.auth.application.port.out;

import java.util.Date;

/**
 * <h2>인증 아웃포트</h2>
 * <p>JWT 토큰 관련 기능을 제공하는 아웃포트 인터페이스</p>
 * <p>헥사고날 아키텍처 원칙에 따라 도메인 계층에서 인프라 계층의 기능을 추상화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface AuthPort {

    /**
     * <h3>토큰 만료 시간 조회</h3>
     * <p>JWT 토큰에서 만료 시간을 추출합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 만료 시간
     * @author Jaeik
     * @since 2.0.0
     */
    Date getTokenExpiration(String token);

    /**
     * <h3>토큰 유효성 검증</h3>
     * <p>JWT 토큰의 유효성을 검증합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 유효성 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean validateToken(String token);

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>JWT 토큰을 해시화하여 블랙리스트 저장용 키를 생성합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 해시값
     * @author Jaeik
     * @since 2.0.0
     */
    String generateTokenHash(String token);
}