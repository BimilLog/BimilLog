package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.infrastructure.adapter.out.auth.KakaoTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>카카오 토큰 조회 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 카카오 토큰 조회 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalKakaoTokenQueryPort를 구현하여 도메인 간 카카오 토큰 조회 기능을 통합 제공합니다.</p>
 * <p>KakaoTokenRepository를 통해 실제 데이터베이스에서 조회합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalKakaoTokenQueryAdapter implements GlobalKakaoTokenQueryPort {

    private final KakaoTokenRepository kakaoTokenRepository;

    /**
     * <h3>사용자 ID로 카카오 토큰 조회</h3>
     * <p>특정 사용자의 카카오 OAuth 토큰을 조회합니다.</p>
     * <p>KakaoTokenRepository를 통해 데이터베이스에서 조회합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return Optional&lt;KakaoToken&gt; 조회된 카카오 토큰 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<KakaoToken> findByUserId(Long userId) {
        return kakaoTokenRepository.findByUserId(userId);
    }
}