
package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
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
 * <p>SocialLoginUseCase의 구현체 소셜 로그인을 처리하는 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final SocialPort socialPort;
    private final SaveUserPort saveUserPort;
    private final RedisUserDataPort redisUserDataPort;
    private final BlacklistPort blacklistPort;

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>소셜 로그인 요청을 처리하고 로그인 결과를 반환합니다.</p>
     * <p>기존 사용자는 쿠키를 생성하고, 신규 사용자는 임시 데이터를 저장한 후 UUID를 반환합니다.</p>
     *
     * @param provider 소셜 제공자
     * @param code     인가 코드
     * @param fcmToken Firebase Cloud Messaging 토큰
     * @return 타입 안전성이 보장된 로그인 결과
     * @throws AuthCustomException 블랙리스트 사용자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        LoginResult.SocialLoginData loginResult = socialPort.login(provider, code);
        LoginResult.SocialUserProfile userProfile = loginResult.userProfile();

        if (blacklistPort.existsByProviderAndSocialId(provider, userProfile.socialId())) {
            throw new AuthCustomException(AuthErrorCode.BLACKLIST_USER);
        }

        if (!loginResult.isNewUser()) {
            return handleExistingUser(loginResult, fcmToken);
        } else {
            return handleNewUser(loginResult, fcmToken);
        }
    }

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 사용자의 로그인 결과를 처리하고 쿠키를 생성합니다.</p>
     *
     * @param loginResult 로그인 결과
     * @param fcmToken    Firebase Cloud Messaging 토큰
     * @return 기존 사용자 로그인 응답
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult.ExistingUser handleExistingUser(LoginResult.SocialLoginData loginResult, String fcmToken) {
        List<ResponseCookie> cookies = saveUserPort.handleExistingUserLogin(
                loginResult.userProfile(), loginResult.token(), fcmToken
        );
        return new LoginResult.ExistingUser(cookies);
    }

    /**
     * <h3>신규 사용자 로그인 처리</h3>
     * <p>신규 사용자의 로그인 결과를 처리하고 임시 데이터를 저장합니다.</p>
     *
     * @param loginResult 로그인 결과
     * @param fcmToken Firebase Cloud Messaging 토큰
     * @return 신규 사용자 로그인 응답
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult.NewUser handleNewUser(LoginResult.SocialLoginData loginResult, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        redisUserDataPort.saveTempData(uuid, loginResult.userProfile(), loginResult.token(), fcmToken);
        ResponseCookie tempCookie = redisUserDataPort.createTempCookie(uuid);
        return new LoginResult.NewUser(uuid, tempCookie);
    }

    /**
     * <h3>로그인 유효성 검사</h3>
     * <p>현재 사용자가 로그인 상태인지 확인합니다.</p>
     * <p>로그인 상태라면 예외를 발생시킵니다.</p>
     *
     * @throws AuthCustomException 이미 로그인 상태인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            throw new AuthCustomException(AuthErrorCode.ALREADY_LOGIN);
        }
    }
}
