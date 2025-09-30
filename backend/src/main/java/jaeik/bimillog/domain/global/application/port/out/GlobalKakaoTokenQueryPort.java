package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.auth.entity.KakaoToken;

import java.util.Optional;

/**
 * <h2>카카오 토큰 조회 공용 포트</h2>
 * <p>여러 도메인에서 공통으로 사용하는 카카오 토큰 조회 기능을 제공하는 포트입니다.</p>
 * <p>카카오 API 호출 시 액세스 토큰이 필요한 경우 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface GlobalKakaoTokenQueryPort {

    /**
     * <h3>사용자 ID로 카카오 토큰 조회</h3>
     * <p>특정 사용자의 카카오 OAuth 토큰을 조회합니다.</p>
     * <p>User와 1:1 관계를 가지므로 하나의 KakaoToken만 존재합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return Optional&lt;KakaoToken&gt; 조회된 카카오 토큰 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<KakaoToken> findByUserId(Long userId);
}