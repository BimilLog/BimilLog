package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenCommandPort;
import jaeik.bimillog.infrastructure.adapter.out.auth.KakaoTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>카카오 토큰 명령 어댑터</h2>
 * <p>카카오 OAuth 토큰 쓰기 기능을 구현하는 어댑터입니다.</p>
 * <p>KakaoTokenCommandPort를 구현하여 카카오 토큰 저장/업데이트/삭제 기능을 제공합니다.</p>
 * <p>KakaoTokenRepository를 통해 실제 데이터베이스 작업을 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalKakaoTokenCommandAdapter implements GlobalKakaoTokenCommandPort {

    private final KakaoTokenRepository kakaoTokenRepository;

    /**
     * <h3>카카오 토큰 저장</h3>
     * <p>새로운 카카오 토큰을 저장합니다.</p>
     * <p>KakaoTokenRepository를 통해 데이터베이스에 저장합니다.</p>
     *
     * @param kakaoToken 저장할 카카오 토큰 엔티티
     * @return KakaoToken 저장된 카카오 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public KakaoToken save(KakaoToken kakaoToken) {
        return kakaoTokenRepository.save(kakaoToken);
    }

    /**
     * <h3>카카오 토큰 삭제</h3>
     * <p>회원 탈퇴 시 카카오 토큰을 삭제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteByMemberId(Long memberId) {
        kakaoTokenRepository.deleteByMemberId(memberId);
    }
}