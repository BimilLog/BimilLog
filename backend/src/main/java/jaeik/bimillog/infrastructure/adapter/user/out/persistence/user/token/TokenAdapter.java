package jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.token;

import jaeik.bimillog.domain.user.application.port.out.TokenPort;
import jaeik.bimillog.domain.user.entity.Token;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * <h2>토큰 영속성 어댑터</h2>
 * <p>토큰 정보 영속성 관리를 위한 출력 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class TokenAdapter implements TokenPort {

    private final TokenRepository tokenRepository;

    /**
     * <h3>토큰 ID로 조회</h3>
     * <p>주어진 ID로 토큰 정보를 조회합니다. ID가 null인 경우 빈 Optional을 반환합니다.</p>
     *
     * @param id 토큰 ID
     * @return Optional<Token> 조회된 토큰 객체. 존재하지 않거나 ID가 null이면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Token> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return tokenRepository.findById(id);
    }

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param token 저장할 토큰 엔티티
     * @return Token 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Token save(Token token) {
        return tokenRepository.save(token);
    }

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
    @Override
    public List<Token> findByUsersId(Long userId) {
        return tokenRepository.findByUsersId(userId);
    };

    /**
     * <h3>사용자 ID로 모든 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByUserId(Long userId) {
        tokenRepository.deleteAllByUserId(userId);
    }
}