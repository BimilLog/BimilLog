package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.Token;

/**
 * <h2>토큰 명령 포트</h2>
 * <p>토큰 저장, 토큰 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface TokenCommandPort {

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     * <p>새 토큰 생성이나 기존 토큰 정보 업데이트에 사용됩니다.</p>
     *
     * @param token 저장할 토큰 엔티티
     * @return Token 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Token save(Token token);

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     * <p>{@link WithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteTokens(Long userId, Long tokenId);

}