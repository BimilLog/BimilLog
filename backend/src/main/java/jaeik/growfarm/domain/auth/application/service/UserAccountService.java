
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.auth.application.port.out.*;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAccountService implements SignUpUseCase, LogoutUseCase, WithdrawUseCase {

    private final LoadUserPort loadUserPort;
    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;
    private final ManageNotificationPort manageNotificationPort;
    private final LoadTokenPort loadTokenPort;
    private final AuthCookieManager authCookieManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        TemporaryUserDataDTO tempUserData = manageTemporaryDataPort.getTempData(uuid);
        if (tempUserData == null) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        }
        return manageAuthDataPort.saveNewUser(userName, uuid, tempUserData.getSocialLoginUserData(),
                tempUserData.getTokenDTO(), tempUserData.getFcmToken());
    }

    @Override
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            logoutSocial(userDetails);
            manageAuthDataPort.logoutUser(userDetails.getUserId());
            manageNotificationPort.deleteAllEmitterByUserId(userDetails.getUserId());
            SecurityContextHolder.clearContext();
            return manageNotificationPort.getLogoutCookies();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGOUT_FAIL, e);
        }
    }

    @Override
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }

        User user = loadUserPort.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        socialLoginPort.unlink(user.getProvider(), user.getSocialId());
        manageAuthDataPort.performWithdrawProcess(userDetails.getUserId());

        eventPublisher.publishEvent(new UserWithdrawnEvent(user.getId()));

        return logout(userDetails);
    }

    @Transactional(readOnly = true)
    public void logoutSocial(CustomUserDetails userDetails) {
        loadTokenPort.findById(userDetails.getTokenId())
                .ifPresent(token -> {
                    User user = token.getUsers();
                    if (user != null) {
                        socialLoginPort.logout(user.getProvider(), token.getAccessToken());
                    }
                });
    }
}
