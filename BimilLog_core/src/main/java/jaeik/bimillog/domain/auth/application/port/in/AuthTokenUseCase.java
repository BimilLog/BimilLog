package jaeik.bimillog.domain.auth.application.port.in;

import jaeik.bimillog.domain.auth.service.SocialWithdrawService;

public interface AuthTokenUseCase {

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     * <p>{@link SocialWithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param memberId  사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteTokens(Long memberId, Long tokenId);
}

