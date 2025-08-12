
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.global.domain.SocialProvider;
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

/**
 * <h2>소셜 로그인 서비스</h2>
 * <p>SocialLonginUseCase의 구현체 소셜 로그인을 처리하는 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final UserQueryUseCase userQueryUseCase;
    private final SocialLoginPort socialLoginPort;
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 요청을 처리하고, 기존 사용자라면 인증 정보를 저장하고 쿠키를 반환하며,
     * 새로운 사용자라면 임시 UUID를 반환</p>
     *
     * @param provider 소셜 제공자 (예: KAKAO, NAVER 등)
     * @param code     소셜 로그인 인증 코드
     * @param fcmToken Firebase Cloud Messaging 토큰 (선택적)
     * @return 로그인 응답 DTO 또는 임시 UUID
     * @since 2.0.0
     */
    @Override
    public LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        LoginResultDTO loginResult = socialLoginPort.login(provider, code);
        SocialLoginUserData userData = loginResult.userData();
        TokenDTO tokenDTO = loginResult.tokenDTO();

        Optional<User> existingUser = userQueryUseCase.findByProviderAndSocialId(provider, userData.getSocialId());

        if (existingUser.isPresent()) {
            return handleExistingUserLogin(existingUser.get(), userData, tokenDTO, fcmToken);
        } else {
            return handleNewUserLogin(userData, tokenDTO, fcmToken);
        }
    }

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 사용자의 소셜 로그인 정보를 저장하고, 인증 쿠키를 반환</p>
     *
     * @param user 기존 사용자 정보
     * @param userData 소셜 로그인 사용자 데이터
     * @param tokenDTO 토큰 정보
     * @param fcmToken Firebase Cloud Messaging 토큰 (선택적)
     * @return 로그인 응답 DTO (기존 사용자)
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResponseDTO<List<ResponseCookie>> handleExistingUserLogin(User user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        List<ResponseCookie> cookies = manageAuthDataPort.saveExistUser(user, userData, tokenDTO, fcmToken);
        return LoginResponseDTO.existingUser(cookies);
    }

    /**
     * <h3>신규 사용자 로그인 처리</h3>
     * <p>신규 사용자의 소셜 로그인 정보를 임시 데이터로 저장하고, 임시 UUID를 반환</p>
     *
     * @param userData 소셜 로그인 사용자 데이터
     * @param tokenDTO 토큰 정보
     * @param fcmToken Firebase Cloud Messaging 토큰 (선택적)
     * @return 로그인 응답 DTO (신규 사용자)
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResponseDTO<String> handleNewUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        String uuid = manageTemporaryDataPort.saveTempData(
                userData, tokenDTO, fcmToken
        );
        return new LoginResponseDTO<>(LoginResponseDTO.LoginType.NEW_USER, uuid);
    }

    /**
     * <h3>로그인 유효성 검사</h3>
     * <p>현재 사용자가 이미 로그인 상태인지 확인하고, 로그인 상태라면 예외를 발생시킴</p>
     *
     * @throws CustomException 이미 로그인 상태인 경우
     * @since 2.0.0
     * @author Jaeik
     */
    private static void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            throw new CustomException(ErrorCode.ALREADY_LOGIN);
        }
    }
}
