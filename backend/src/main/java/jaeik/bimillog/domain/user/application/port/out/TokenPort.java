package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.user.entity.Token;

import java.util.List;
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

    /**
     * <h3>사용자 ID로 모든 토큰 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 토큰을 조회합니다.</p>
     * <p>회원 탈퇴 시 모든 토큰을 블랙리스트에 등록하기 위해 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return 사용자의 모든 토큰 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Token> findByUsersId(Long userId);

    /**
     * <h3>사용자 ID로 모든 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByUserId(Long userId);
}