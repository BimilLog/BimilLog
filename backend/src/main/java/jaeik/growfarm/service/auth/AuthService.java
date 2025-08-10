package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.kakao.KakaoCheckConsentDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.BlackListRepository;
import jaeik.growfarm.repository.user.UserJdbcRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.kakao.KakaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * <h2>AuthService 클래스</h2>
 * <p>
 * 인증 관련 비즈니스 로직을 처리한다.
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlackListRepository blackListRepository;
    private final EmitterRepository emitterRepository;
    private final TempUserDataManager tempUserDataManager;
    private final AuthUpdateService authUpdateService;
    private final UserJdbcRepository userJdbcRepository;
    private final TokenRepository tokenRepository;
    private final SocialLoginManager socialLoginManager;

    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        socialLoginManager.unlink(event.getProvider(), event.getSocialId());
    }

    /**
     * <h3>소셜 로그인</h3>
     *
     * <p>
     * 기존 회원은 Jwt가 담긴 쿠키 쌍을 반환하고, 신규 회원은 UUID가 담긴 임시 쿠키를 반환한다.
     * </p>
     *
     * @param provider 소셜 로그인 제공자
     * @param code     프론트에서 반환된 인가 코드
     * @param fcmToken Firebase Cloud Messaging 토큰
     * @return Jwt가 삽입된 쿠키 또는 토큰 ID
     * @author Jaeik
     * @since 3.0.0
     */
    public LoginResponseDTO<?> processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        SocialLoginStrategy.LoginResult loginResult = socialLoginManager.login(provider, code);
        SocialLoginUserData userData = loginResult.userData();
        TokenDTO tokenDTO = loginResult.tokenDTO();

        Optional<Users> existingUser = checkExistingUser(provider, userData.getSocialId());

        return existingUser
                .map(user -> handleExistingUserLogin(user, userData, tokenDTO, fcmToken))
                .orElseGet(() -> handleNewUserLogin(userData, tokenDTO, fcmToken));
    }

    private LoginResponseDTO<?> handleExistingUserLogin(Users user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        return existingUserLogin(user, userData, tokenDTO, fcmToken);
    }

    private LoginResponseDTO<?> handleNewUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        if (checkBlackList(userData.getSocialId(), userData.getProvider())) {
            throw new CustomException(ErrorCode.BLACKLIST_USER);
        }
        return newUserLogin(userData, tokenDTO, fcmToken);
    }

    /**
     * <h3>로그인 유효성 검사</h3>
     *
     * <p>
     * 현재 로그인 상태인지 확인하고, 이미 로그인 된 경우 예외를 발생시킨다.
     * </p>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    private static void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            throw new CustomException(ErrorCode.ALREADY_LOGIN);
        }
    }

    private Optional<Users> checkExistingUser(SocialProvider provider, String socialId) {
        return userRepository.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>블랙리스트 사용자 확인</h3>
     *
     * <p>
     * 소셜 ID와 제공자 정보로 블랙리스트에 있는지 확인한다.
     * </p>
     *
     * @param socialId 소셜 ID
     * @param provider 제공자 정보
     * @return boolean 블랙리스트 여부
     * @author Jaeik
     * @since 3.0.0
     */
    private boolean checkBlackList(String socialId, SocialProvider provider) {
        return blackListRepository.existsByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     *
     * <p>
     * 기존 사용자의 정보를 업데이트하고 JWT 토큰을 생성하여 쿠키를 반환한다.
     * </p>
     *
     * @param user         기존 사용자 정보
     * @param kakaoInfoDTO 카카오 사용자 정보
     * @param tokenDTO     카카오 토큰 정보
     * @param fcmToken     Firebase Cloud Messaging 토큰
     * @return LoginResponse<List < ResponseCookie>> JWT가 삽입된 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    private LoginResponseDTO<List<ResponseCookie>> existingUserLogin(Users user, SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        List<ResponseCookie> cookies = authUpdateService.saveExistUser(user, userData, tokenDTO, fcmToken);
        return LoginResponseDTO.existingUser(cookies);
    }

    /**
     * <h3>신규 사용자 로그인 처리</h3>
     *
     * <p>
     * 신규 사용자의 정보를 TempUserDataManager 메모리에 임시저장하고 UUID를 담은 쿠키를 반환한다.
     * </p>
     *
     * @param kakaoInfoDTO 카카오 사용자 정보
     * @param tokenDTO     카카오 토큰 정보
     * @param fcmToken     Firebase Cloud Messaging 토큰
     * @return LoginResponse<ResponseCookie> UUID가 삽입된 쿠키
     * @author Jaeik
     * @since 1.0.0
     */
    private LoginResponseDTO<ResponseCookie> newUserLogin(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        String uuid = tempUserDataManager.saveTempData(userData, tokenDTO, fcmToken);
        return LoginResponseDTO.newUser(uuid);
    }

    /**
     * <h3>회원 가입</h3>
     *
     * <p>
     * uuid로 임시 저장된 사용자 정보를 기반으로 회원가입을 처리한다.
     * </p>
     *
     * @param userName 사용자가 설정한 닉네임
     * @param uuid     임시 저장된 사용자 정보의 UUID
     * @return JWT가 삽입된 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    public List<ResponseCookie> signUp(String userName, String uuid) {
        TempUserDataManager.TempUserData tempUserData = tempUserDataManager.getTempData(uuid);
        if (tempUserData == null) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        }
        return authUpdateService.saveNewUser(userName, uuid, tempUserData.getSocialLoginUserData(), tempUserData.getTokenDTO(),
                tempUserData.getFcmToken());
    }

    /**
     * <h3>로그아웃</h3>
     *
     * <p>
     * 소셜 로그아웃, DB 토큰 삭제, SSE 연결 삭제, SecurityContext 클리어 등 모든 로그아웃 처리를 수행한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 로그아웃 쿠키 리스트
     * @author Jaeik
     * @since 3.0.0
     */
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            // 1. 소셜 로그아웃
            logoutSocial(userDetails);
            // 2. DB에서 사용자 토큰 정보 삭제 (FCM 토큰 포함)
            authUpdateService.logoutUser(userDetails.getUserId());
            // 3. SSE 연결 삭제
            emitterRepository.deleteAllEmitterByUserId(userDetails.getUserId());
            // 4. SecurityContext 클리어
            SecurityContextHolder.clearContext();
            // 5. 로그아웃 쿠키 반환
            return jwtTokenProvider.getLogoutCookies();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGOUT_FAIL, e);
        }
    }

    /**
     * <h3>회원탈퇴</h3>
     *
     * <p>
     * 사용자의 모든 데이터를 삭제하고 카카오 연결을 해제합니다..
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 탈퇴 처리 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }

        Users user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        socialLoginManager.unlink(user.getProvider(), user.getSocialId());
        authUpdateService.performWithdrawProcess(userDetails.getUserId());

        return logout(userDetails);
    }

    /**
     * <h3>소셜 로그아웃</h3>
     *
     * <p>
     * 각 소셜 서버와 통신하여 로그아웃을 수행합니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 3.0.0
     */
    @Transactional(readOnly = true)
    public void logoutSocial(CustomUserDetails userDetails) {
        tokenRepository.findById(userDetails.getTokenId())
                .ifPresent(token -> {
                    Users user = token.getUser();
                    if (user != null) {
                        socialLoginManager.logout(user.getProvider(), token.getAccessToken());
                    }
                });
    }
}
