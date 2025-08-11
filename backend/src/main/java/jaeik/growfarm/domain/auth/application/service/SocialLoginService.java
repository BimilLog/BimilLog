
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final LoadUserPort loadUserPort;
    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;

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
}
