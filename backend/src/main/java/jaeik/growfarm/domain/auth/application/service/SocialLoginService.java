
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.out.BlacklistPort;
import jaeik.growfarm.domain.auth.application.port.out.SaveUserPort;
import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.auth.in.web.dto.LoginResponseDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResultDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
    private final SaveUserPort saveUserPort;
    private final TempDataPort tempDataPort;
    private final BlacklistPort blacklistPort;

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 요청을 처리하고 로그인 결과를 반환합니다.</p>
     * <p>기존 사용자는 쿠키를 생성하고, 신규 사용자는 임시 데이터를 저장한 후 UUID를 반환합니다.</p>
     *
     * @param provider 소셜 제공자
     * @param code     인가 코드
     * @param fcmToken Firebase Cloud Messaging 토큰
     * @return 로그인 응답 DTO
     * @throws CustomException 블랙리스트 사용자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        LoginResultDTO loginResult = socialLoginPort.login(provider, code);
        SocialLoginUserData userData = loginResult.getUserData();
        
        if (blacklistPort.existsByProviderAndSocialId(provider, userData.socialId())) {
            throw new CustomException(ErrorCode.BLACKLIST_USER);
        }

        if (loginResult.getLoginType() == LoginResultDTO.LoginType.EXISTING_USER) {
            return handleExistingUser(loginResult, fcmToken);

        } else {
            return handleNewUser(loginResult);
        }
    }
    
    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 사용자의 로그인 결과를 처리하고 쿠키를 생성합니다.</p>
     *
     * @param loginResult 로그인 결과 DTO
     * @param fcmToken    Firebase Cloud Messaging 토큰
     * @return 로그인 응답 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResponseDTO<List<ResponseCookie>> handleExistingUser(LoginResultDTO loginResult, String fcmToken) {
        List<ResponseCookie> cookies = saveUserPort.handleExistingUserLogin(
                loginResult.getUserData(), loginResult.getTokenDTO(), fcmToken
        );
        return new LoginResponseDTO<>(LoginResponseDTO.LoginType.EXISTING_USER, cookies);
    }
    
    /**
     * <h3>신규 사용자 로그인 처리</h3>
     * <p>신규 사용자의 로그인 결과를 처리하고 임시 데이터를 저장합니다.</p>
     *
     * @param loginResult 로그인 결과 DTO
     * @return 로그인 응답 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResponseDTO<ResponseCookie> handleNewUser(LoginResultDTO loginResult) {
        String uuid = UUID.randomUUID().toString();
        ResponseCookie tempCookie = tempDataPort.saveTempDataAndCreateCookie(uuid, loginResult.getUserData(), loginResult.getTokenDTO());
        return new LoginResponseDTO<>(LoginResponseDTO.LoginType.NEW_USER, tempCookie);
    }
    
    /**
     * <h3>로그인 유효성 검사</h3>
     * <p>현재 사용자가 로그인 상태인지 확인합니다.</p>
     * <p>로그인 상태라면 예외를 발생시킵니다.</p>
     *
     * @throws CustomException 이미 로그인 상태인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.ALREADY_LOGIN);
        }
    }
}
