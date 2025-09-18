package jaeik.bimillog.infrastructure.adapter.out.user;

import jaeik.bimillog.domain.auth.application.service.LogoutService;
import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.user.application.service.WithdrawService;
import jaeik.bimillog.domain.user.entity.BlackList;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.BlackListRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.TokenRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthCookieManager;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자 삭제 어댑터</h2>
 * <p>사용자 삭제 및 로그아웃 처리를 위한 영속성 어댑터</p>
 * <p>로그아웃 처리, 회원 탈퇴 처리, 블랙리스트 저장</p>
 * <p>로그아웃 쿠키 생성, 다중 로그인 지원</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class DeleteUserAdapter implements DeleteUserPort {

    private final EntityManager entityManager;
    private final TokenRepository tokenRepository;
    private final AuthCookieManager authCookieManager;
    private final UserRepository userRepository;
    private final BlackListRepository blackListRepository;

    /**
     * <h3>로그아웃 처리</h3>
     * <p>다중 로그인 환경에서 특정 토큰만 삭제하여 해당 기기만 로그아웃 처리합니다.</p>
     * <p>이벤트는 호출하는 측에서 발행하므로 여기서는 토큰 삭제만 담당</p>
     * <p>{@link WithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
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
        // 이벤트 발행은 호출하는 측(LogoutService)에서 담당
    }

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행합니다.</p>
     * <p>{@link WithdrawService}에서 회원 탈퇴 처리 시 호출됩니다.</p>
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
        userRepository.deleteById(userId);
    }

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     * <p>사용자 로그아웃 시 JWT 토큰을 무효화하는 쿠키를 생성</p>
     * <p>{@link WithdrawService}, {@link LogoutService}에서 로그아웃 쿠키 생성 시 호출됩니다.</p>
     *
     * @return 로그아웃 쿠키 리스트 (Access Token, Refresh Token 무효화 쿠키)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> getLogoutCookies() {
        return authCookieManager.getLogoutCookies();
    }

    /**
     * <h3>블랙리스트 저장</h3>
     * <p>블랙리스트에 사용자 정보를 저장합니다.</p>
     * <p>{@link WithdrawService}에서 회원 탈퇴 시 블랙리스트 등록을 위해 호출됩니다.</p>
     *
     * @param blackList 저장할 블랙리스트 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void saveBlackList(BlackList blackList) {
        blackListRepository.save(blackList);
    }

}
