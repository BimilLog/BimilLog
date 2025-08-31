package jaeik.bimillog.domain.auth.application.port.out;

/**
 * <h2>인증 포트</h2>
 * <p>JWT 토큰 관련 인증 기능을 추상화한 아웃포트</p>
 * <p>헥사고날 아키텍처에서 인프라스트럭처 계층의 인증 기능을 추상화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface AuthPort {

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>주어진 토큰에 대한 SHA-256 해시값을 생성합니다.</p>
     *
     * @param token 해시화할 토큰
     * @return 생성된 해시값
     * @since 2.0.0
     * @author Jaeik
     */
    String generateTokenHash(String token);
}