package jaeik.bimillog.domain.auth.application.port.in;

/**
 * <h2>카카오 토큰 명령 유스케이스</h2>
 * <p>카카오 OAuth 토큰 삭제 기능을 제공하는 인바운드 포트입니다.</p>
 * <p>로그아웃, 회원탈퇴, 사용자 차단 시 카카오 토큰 정리를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface KakaoTokenUseCase {

    /**
     * <h3>사용자 ID로 카카오 토큰 삭제</h3>
     * <p>특정 사용자의 카카오 OAuth 토큰을 삭제합니다.</p>
     * <p>로그아웃, 회원탈퇴, 사용자 차단 시 호출되어 카카오 토큰을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserId(Long userId);
}