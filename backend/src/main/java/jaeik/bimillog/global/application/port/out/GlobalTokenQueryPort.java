package jaeik.bimillog.global.application.port.out;

import jaeik.bimillog.domain.auth.entity.Token;

import java.util.List;
import java.util.Optional;

/**
 * <h2>토큰 조회 공용 포트</h2>
 * <p>여러 도메인에서 공통으로 사용하는 토큰 조회 기능을 제공하는 포트입니다.</p>
 * <p>토큰 ID 조회, 사용자별 토큰 목록 조회</p>
 * <p>Auth 도메인과 User 도메인에서 중복 사용되는 토큰 조회 기능을 통합합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface GlobalTokenQueryPort {

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     * <p>특정 ID에 해당하는 토큰 엔티티를 조회합니다.</p>
     * <p>토큰 유효성 검증이나 토큰 관련 비즈니스 로직 실행에 사용됩니다.</p>
     *
     * @param tokenId 조회할 토큰 ID
     * @return Optional&lt;Token&gt; 조회된 토큰 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Token> findById(Long tokenId);

    /**
     * <h3>사용자의 모든 토큰 조회</h3>
     * <p>특정 사용자가 소유한 모든 토큰을 조회합니다.</p>
     * <p>사용자 차단, 회원 탈퇴 시 모든 토큰을 블랙리스트에 등록하거나 삭제할 때 사용됩니다.</p>
     *
     * @param userId 토큰을 조회할 사용자 ID
     * @return List&lt;Token&gt; 해당 사용자의 모든 토큰 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Token> findAllByUserId(Long userId);
}