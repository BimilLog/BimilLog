
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.out.AuthPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.auth.TemporaryUserDataDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.domain.SocialProvider;
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
import java.util.UUID;

/**
 * <h2>소셜 로그인 서비스</h2>
 * <p>SocialLonginUseCase의 구현체 소셜 로그인을 처리하는 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final AuthPort authPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 인증 코드를 통해 사용자를 인증하고 로그인 쿠키를 생성</p>
     * <p>신규 사용자는 임시 쿠키를 발행하고 회원가입을 유도</p>
     *
     * @param provider 소셜 제공자
     * @param code     인가 코드
     * @return 로그인 응답 DTO (로그인 쿠키 또는 임시 쿠키)
     * @since 2.1.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        LoginResultDTO loginResult = socialLoginPort.login(provider, code);
        SocialLoginUserData userData = loginResult.getUserData();
        TokenDTO tokenDTO = loginResult.getTokenDTO();

        if (loginResult.getLoginType() == LoginResultDTO.LoginType.EXISTING_USER) {
            // 기존 사용자 처리
            List<ResponseCookie> cookies = manageAuthDataPort.handleExistingUserLogin(userData, tokenDTO, fcmToken);

            return new LoginResponseDTO<>(LoginResponseDTO.LoginType.EXISTING_USER, cookies);
        } else {
            // 신규 사용자 처리
            String uuid = UUID.randomUUID().toString();
            manageTemporaryDataPort.saveTempData(uuid, userData, tokenDTO);
            ResponseCookie tempCookie = authPort.createTempCookie(uuid);
            return new LoginResponseDTO<>(LoginResponseDTO.LoginType.NEW_USER, tempCookie);
        }
    }

    private void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.ALREADY_LOGIN);
        }
    }

    @Override
    @Transactional
    public LoginResponseDTO<List<ResponseCookie>> registerNewUser(String userName, String uuid) {
        TemporaryUserDataDTO temporaryUserData = manageTemporaryDataPort.getTempData(uuid)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TEMP_DATA));

        List<ResponseCookie> cookies = manageAuthDataPort.saveNewUser(userName, uuid, temporaryUserData.getSocialLoginUserData(), temporaryUserData.getTokenDTO(), temporaryUserData.getFcmToken());

        return new LoginResponseDTO<>(LoginResponseDTO.LoginType.NEW_USER_REGISTERED, cookies);
    }
}
