package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.auth.application.port.out.*;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.global.event.UserLoggedOutEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * <h2>사용자 계정 서비스</h2>
 * <p>회원 가입, 로그아웃, 회원 탈퇴 등의 사용자 계정 관련 기능을 처리하는 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class UserAccountService implements SignUpUseCase, LogoutUseCase, WithdrawUseCase {
    private final ManageTemporaryDataPort manageTemporaryDataPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final SocialLoginPort socialLoginPort;
    private final UserQueryUseCase userQueryUseCase;
    private final LoadTokenPort loadTokenPort;
    private final AuthPort authPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>회원 가입 처리</h3>
     * <p>임시 UUID를 사용하여 새로운 사용자를 등록하고, FCM 토큰이 존재하면 이벤트를 발행합니다.</p>
     *
     * @param userName 사용자의 이름
     * @param uuid     임시 UUID
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        Optional<TemporaryUserDataDTO> tempUserData = manageTemporaryDataPort.getTempData(uuid);

        if (tempUserData.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        } else {
            return manageAuthDataPort.saveNewUser(userName, uuid, tempUserData.get().socialLoginUserData, tempUserData.get().tokenDTO, tempUserData.get().getFcmToken());
        }
    }

    /**
     * <h3>로그아웃 처리</h3>
     * <p>사용자를 로그아웃하고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            logoutSocial(userDetails);
            eventPublisher.publishEvent(UserLoggedOutEvent.of(userDetails.getUserId(), userDetails.getTokenId()));
            SecurityContextHolder.clearContext();
            return authPort.getLogoutCookies();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGOUT_FAIL, e);
        }
    }

    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행하며, 이벤트를 발행합니다.</p>
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
        User user = userQueryUseCase.findById(userDetails.getUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        socialLoginPort.unlink(user.getProvider(), user.getSocialId());
        manageAuthDataPort.performWithdrawProcess(userDetails.getUserId());
        eventPublisher.publishEvent(new UserWithdrawnEvent(user.getId()));

        return logout(userDetails);
    }

    /**
     * <h3>소셜 로그아웃 처리</h3>
     * <p>사용자의 소셜 로그아웃을 수행합니다.</p>
     *
     * @param userDetails 현재 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Transactional(readOnly = true)
    public void logoutSocial(CustomUserDetails userDetails) {
        loadTokenPort.findById(userDetails.getTokenId()).ifPresent(token -> {
            User user = token.getUsers();
            if (user != null) {
                socialLoginPort.logout(user.getProvider(), token.getAccessToken());
            }
        });
    }
}
