package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.user.entity.Token;

import java.util.Optional;

/**
 * <h2>토큰 영속성 포트</h2>
 * <p>토큰 정보 영속성 처리를 위한 출력 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface TokenPort {

    /**
     * <h3>토큰 ID로 조회</h3>
     * <p>주어진 ID로 토큰 정보를 조회합니다.</p>
     *
     * @param id 토큰 ID
     * @return Optional<Token> 조회된 토큰 객체
     * @author Jaeik
     * @since  2.0.0
     */
    Optional<Token> findById(Long id);

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param token 저장할 토큰 엔티티
     * @return Token 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Token save(Token token);
}