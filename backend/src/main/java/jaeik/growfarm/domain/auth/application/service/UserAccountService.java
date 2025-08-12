
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.auth.application.port.out.*;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.global.event.FcmTokenRegisteredEvent;
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

    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        TemporaryUserDataDTO tempUserData = manageTemporaryDataPort.getTempData(uuid);
        if (tempUserData == null) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        }
        
        List<ResponseCookie> cookies = manageAuthDataPort.saveNewUser(userName, uuid, 
                tempUserData.getSocialLoginUserData(), tempUserData.getTokenDTO());
        
        // FCM 토큰이 존재하면 이벤트 발행
        if (tempUserData.getFcmToken() != null && !tempUserData.getFcmToken().isEmpty()) {
            // 사용자 ID는 쿠키에서 추출하거나 다른 방법으로 가져와야 함
            // 여기서는 UserRepository를 통해 방금 저장된 사용자를 찾는 방식을 사용
            User user = userQueryUseCase.findByProviderAndSocialId(
                    tempUserData.getSocialLoginUserData().getProvider(),
                    tempUserData.getSocialLoginUserData().getSocialId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
            eventPublisher.publishEvent(
                    FcmTokenRegisteredEvent.of(user.getId(), tempUserData.getFcmToken()));
        }
        
        return cookies;
    }

    @Override
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            logoutSocial(userDetails);
            eventPublisher.publishEvent(UserLoggedOutEvent.of(
                userDetails.getUserId(),
                userDetails.getTokenId()
            ));
            SecurityContextHolder.clearContext();
            return authPort.getLogoutCookies();
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
        User user = userQueryUseCase.findById(userDetails.getUserId())
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
