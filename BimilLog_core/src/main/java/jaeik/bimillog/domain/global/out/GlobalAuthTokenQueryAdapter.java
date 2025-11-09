package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.out.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * <h2>JWT 토큰 조회 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 JWT 토큰 조회 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalTokenQueryPort를 구현하여 도메인 간 JWT 토큰 조회 기능을 통합 제공합니다.</p>
 * <p>JwtTokenRepository를 통해 실제 JWT 토큰 데이터에 접근합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalAuthTokenQueryAdapter {

    private final AuthTokenRepository authTokenRepository;

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     * <p>특정 ID에 해당하는 토큰 엔티티를 조회합니다.</p>
     * <p>TokenRepository를 통해 데이터베이스에서 토큰 정보를 조회합니다.</p>
     *
     * @param tokenId 조회할 토큰 ID
     * @return Optional&lt;AuthToken&gt; 조회된 토큰 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    public Optional<AuthToken> findById(Long tokenId) {
        return authTokenRepository.findById(tokenId);
    }

    /**
     * <h3>사용자의 모든 토큰 조회</h3>
     * <p>특정 사용자가 소유한 모든 토큰을 조회합니다.</p>
     * <p>TokenRepository를 통해 해당 사용자의 모든 토큰을 조회합니다.</p>
     *
     * @param memberId 토큰을 조회할 사용자 ID
     * @return List&lt;AuthToken&gt; 해당 사용자의 모든 토큰 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<AuthToken> findAllByMemberId(Long memberId) {
        return authTokenRepository.findByMemberId(memberId);
    }
}