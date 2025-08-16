
package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.out.CheckBlacklistPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResponseDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResultDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
import jaeik.growfarm.domain.common.entity.SocialProvider;
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
    private final ManageAuthDataPort manageAuthDataPort;
    private final ManageTemporaryDataPort manageTemporaryDataPort;
    private final CheckBlacklistPort checkBlacklistPort;

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
        TokenDTO tokenDTO = loginResult.getTokenDTO();
        
        if (checkBlacklistPort.existsByProviderAndSocialId(provider, userData.socialId())) {
            throw new CustomException(ErrorCode.BLACKLIST_USER);
        }

        if (loginResult.getLoginType() == LoginResultDTO.LoginType.EXISTING_USER) {
            List<ResponseCookie> cookies = manageAuthDataPort.handleExistingUserLogin(userData, tokenDTO, fcmToken);
            return new LoginResponseDTO<>(LoginResponseDTO.LoginType.EXISTING_USER, cookies);
        } else {
            String uuid = UUID.randomUUID().toString();
            manageTemporaryDataPort.saveTempData(uuid, userData, tokenDTO);
            ResponseCookie tempCookie = manageTemporaryDataPort.createTempCookie(uuid);
            return new LoginResponseDTO<>(LoginResponseDTO.LoginType.NEW_USER, tempCookie);
        }
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
