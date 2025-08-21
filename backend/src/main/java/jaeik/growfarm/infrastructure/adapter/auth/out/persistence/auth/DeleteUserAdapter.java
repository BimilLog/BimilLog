package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.growfarm.domain.auth.application.port.out.DeleteUserPort;
import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteUserAdapter implements DeleteUserPort {

    private final EntityManager entityManager;
    private final TokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthCookieManager authCookieManager;
    private final UserCommandUseCase userCommandUseCase;

    /**
     * <h3>로그아웃 처리</h3>
     * <p>다중 로그인 환경에서 특정 토큰만 삭제하여 해당 기기만 로그아웃 처리합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void logoutUser(Long userId, Long tokenId) {
        if (tokenId != null) {
            // 특정 토큰만 삭제 (다중 로그인 유지)
            tokenRepository.deleteById(tokenId);
        } else {
            // tokenId가 null인 경우 (회원탈퇴 등) 모든 토큰 삭제
            tokenRepository.deleteAllByUserId(userId);
        }
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId));
    }

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void performWithdrawProcess(Long userId) {
        entityManager.flush();
        entityManager.clear();

        tokenRepository.deleteAllByUserId(userId);
        userCommandUseCase.deleteById(userId);
    }

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     * <p>사용자 로그아웃 시 JWT 토큰을 무효화하는 쿠키를 생성</p>
     *
     * @return 로그아웃 쿠키 리스트 (Access Token, Refresh Token 무효화 쿠키)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> getLogoutCookies() {
        return authCookieManager.getLogoutCookies();
    }

}
