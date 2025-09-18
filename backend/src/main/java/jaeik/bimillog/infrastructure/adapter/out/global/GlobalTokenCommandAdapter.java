package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.global.application.port.out.GlobalTokenCommandPort;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>토큰 명령 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 토큰 쓰기 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalTokenCommandPort를 구현하여 도메인 간 토큰 쓰기 기능을 통합 제공합니다.</p>
 * <p>TokenRepository를 통해 실제 토큰 데이터를 저장/삭제합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalTokenCommandAdapter implements GlobalTokenCommandPort {

    private final TokenRepository tokenRepository;

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     * <p>TokenRepository를 통해 데이터베이스에 토큰을 저장합니다.</p>
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
     * <h3>사용자 ID로 모든 토큰 삭제</h3>
     * <p>특정 사용자가 소유한 모든 토큰을 삭제합니다.</p>
     * <p>TokenRepository를 통해 해당 사용자의 모든 토큰을 제거합니다.</p>
     *
     * @param userId 토큰을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByUserId(Long userId) {
        tokenRepository.deleteAllByUserId(userId);
    }
}