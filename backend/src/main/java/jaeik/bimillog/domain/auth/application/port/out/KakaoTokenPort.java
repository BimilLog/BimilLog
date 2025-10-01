package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.KakaoToken;

/**
 * <h2>카카오 토큰 명령 포트</h2>
 * <p>카카오 OAuth 토큰 저장, 업데이트, 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface KakaoTokenPort {

    /**
     * <h3>카카오 토큰 저장</h3>
     * <p>새로운 카카오 토큰을 저장합니다.</p>
     * <p>회원가입 또는 첫 로그인 시 호출됩니다.</p>
     *
     * @param kakaoToken 저장할 카카오 토큰 엔티티
     * @return KakaoToken 저장된 카카오 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    KakaoToken save(KakaoToken kakaoToken);

    /**
     * <h3>카카오 토큰 업데이트</h3>
     * <p>로그인 시 갱신된 카카오 토큰 정보를 업데이트합니다.</p>
     * <p>Member ID로 KakaoToken을 조회하여 액세스 토큰과 리프레시 토큰을 갱신합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param kakaoAccessToken 새로운 카카오 액세스 토큰
     * @param kakaoRefreshToken 새로운 카카오 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    void updateTokens(Long memberId, String kakaoAccessToken, String kakaoRefreshToken);

    /**
     * <h3>카카오 토큰 삭제</h3>
     * <p>회원 탈퇴 시 카카오 토큰을 삭제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByMemberId(Long memberId);
}