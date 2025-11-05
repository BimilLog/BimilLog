package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthTokenPort;
import jaeik.bimillog.domain.auth.application.service.SocialWithdrawService;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
public class AuthTokenAdapter implements AuthTokenPort {

    private final AuthTokenRepository authTokenRepository;

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     * <p>{@link SocialWithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void deleteTokens(Long memberId, Long tokenId) {
        if (tokenId != null) {
            authTokenRepository.deleteById(tokenId);
        } else {
            authTokenRepository.deleteAllByMemberId(memberId);
        }
    }
}