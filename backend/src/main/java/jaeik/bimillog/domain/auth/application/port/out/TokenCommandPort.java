package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.Token;

/**
 * <h2>토큰 명령 공용 포트</h2>
 * <p>여러 도메인에서 공통으로 사용하는 토큰 쓰기 기능을 제공하는 포트입니다.</p>
 * <p>토큰 저장, 토큰 삭제</p>
 * <p>Auth 도메인과 User 도메인에서 중복 사용되는 토큰 쓰기 기능을 통합합니다.</p>
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
     * <h3>사용자 ID로 모든 토큰 삭제</h3>
     * <p>특정 사용자가 소유한 모든 토큰을 삭제합니다.</p>
     * <p>회원 탈퇴 시 해당 사용자의 모든 토큰을 완전히 제거할 때 사용됩니다.</p>
     *
     * @param userId 토큰을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByUserId(Long userId);
}