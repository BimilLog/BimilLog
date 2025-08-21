package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.TokenBlacklistUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.auth.application.port.out.DeleteUserPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.event.UserWithdrawnEvent;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>회원 탈퇴 서비스</h2>
 * <p>회원 탈퇴 관련 기능을 처리하는 전용 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class WithdrawService implements WithdrawUseCase {

    private final LoadUserPort loadUserPort;
    private final DeleteUserPort deleteUserPort;
    private final SocialLoginPort socialLoginPort;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenBlacklistUseCase tokenBlacklistUseCase;
    private final LogoutUseCase logoutUseCase;

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     * <p>탈퇴 시 해당 사용자의 모든 JWT 토큰을 블랙리스트에 등록하여 즉시 무효화합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }
        User user = loadUserPort.findById(userDetails.getUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자의 모든 토큰을 블랙리스트에 등록 (보안 강화)
        tokenBlacklistUseCase.blacklistAllUserTokens(user.getId(), "사용자 탈퇴");

        try {
            socialLoginPort.unlink(user.getProvider(), user.getSocialId());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SOCIAL_UNLINK_FAILED, e);
        }
        deleteUserPort.performWithdrawProcess(userDetails.getUserId());
        eventPublisher.publishEvent(new UserWithdrawnEvent(user.getId()));

        return logoutUseCase.logout(userDetails);
    }

    /**
     * <h3>관리자 강제 탈퇴 처리</h3>
     * <p>관리자 권한으로 지정된 사용자를 강제 탈퇴 처리합니다.</p>
     * <p>강제 탈퇴 시 해당 사용자의 모든 JWT 토큰을 블랙리스트에 등록하여 즉시 차단합니다.</p>
     *
     * @param userId 탈퇴시킬 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void forceWithdraw(Long userId) {
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 관리자 강제 탈퇴 시 해당 사용자의 모든 토큰을 즉시 블랙리스트에 등록
        tokenBlacklistUseCase.blacklistAllUserTokens(userId, "관리자 강제 탈퇴");

        try {
            socialLoginPort.unlink(user.getProvider(), user.getSocialId());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SOCIAL_UNLINK_FAILED, e);
        }
        deleteUserPort.performWithdrawProcess(userId);
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
    }
}
