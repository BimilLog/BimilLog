
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.auth.application.port.out.*;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.UserLoggedOutEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
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

    private final LoadUserPort loadUserPort;
    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;
    private final LoadTokenPort loadTokenPort;
    private final AuthPort authPort;
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
            // 1. 소셜 로그아웃 처리 (카카오 등)
            logoutSocial(userDetails);
            
            // 2. 로그아웃 이벤트 발생 (토큰 삭제, SSE 정리 등을 이벤트 리스너에서 처리)
            eventPublisher.publishEvent(UserLoggedOutEvent.of(
                userDetails.getUserId(), 
                userDetails.getTokenId()
            ));
            
            // 3. Spring Security 컨텍스트 초기화
            SecurityContextHolder.clearContext();
            
            // 4. 로그아웃 쿠키 반환 (AuthPort를 통해 인증 도메인에서 처리)
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
