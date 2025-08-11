package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.AuthLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.out.*;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.auth.AuthCookieManager;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>인증 로그인 서비스</h2>
 * <p>소셜 로그인, 회원가입, 로그아웃, 회원탈퇴 관련 비즈니스 로직 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthLoginService implements AuthLoginUseCase {

    private final LoadUserPort loadUserPort;
    private final CheckBlacklistPort checkBlacklistPort;
    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;
    private final ManageNotificationPort manageNotificationPort;
    private final LoadTokenPort loadTokenPort;
    private final AuthCookieManager authCookieManager;

    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        socialLoginPort.unlink(event.getProvider(), event.getSocialId());
    }

    @Override
    public LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        LoginResultDTO loginResult = socialLoginPort.login(provider, code);
        SocialLoginUserData userData = loginResult.userData();
        TokenDTO tokenDTO = loginResult.tokenDTO();

        Optional<User> existingUser = loadUserPort.findByProviderAndSocialId(provider, userData.getSocialId());

        if (existingUser.isPresent()) {
            return handleExistingUserLogin(existingUser.get(), userData, tokenDTO, fcmToken);
        } else {
            return handleNewUserLogin(userData, tokenDTO, fcmToken);
        }
    }

    private LoginResponseDTO<List<ResponseCookie>> handleExistingUserLogin(User user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        List<ResponseCookie> cookies = manageAuthDataPort.saveExistUser(user, userData, tokenDTO, fcmToken);
        return LoginResponseDTO.existingUser(cookies);
    }

    private LoginResponseDTO<String> handleNewUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        String uuid = manageTemporaryDataPort.saveTempData(
                userData, tokenDTO, fcmToken
        );
        return new LoginResponseDTO<>(LoginResponseDTO.LoginType.NEW_USER, uuid);
    }

    private static void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            throw new CustomException(ErrorCode.ALREADY_LOGIN);
        }
    }

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
            // 1. 소셜 로그아웃
            logoutSocial(userDetails);
            // 2. DB에서 사용자 토큰 정보 삭제 (FCM 토큰 포함)
            manageAuthDataPort.logoutUser(userDetails.getUserId());
            // 3. SSE 연결 삭제
            manageNotificationPort.deleteAllEmitterByUserId(userDetails.getUserId());
            // 4. SecurityContext 클리어
            SecurityContextHolder.clearContext();
            // 5. 로그아웃 쿠키 반환
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