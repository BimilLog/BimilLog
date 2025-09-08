package jaeik.bimillog.domain.user.application.port.in;

/**
 * <h2>토큰 정리 유스케이스</h2>
 * <p>사용자 로그아웃 시 토큰 정리와 관련된 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 * <p>다중 기기 로그인 환경에서 특정 토큰만 정리하는 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserLogoutUseCase {

    /**
     * <h3>특정 토큰 정리</h3>
     * <p>사용자 로그아웃 시 특정 토큰만 정리합니다.</p>
     * <p>다중 기기 로그인 환경에서 다른 기기의 로그인 상태는 유지됩니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 정리할 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void cleanupSpecificToken(Long userId, Long tokenId);
}