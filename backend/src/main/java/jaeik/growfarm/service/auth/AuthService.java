package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.BlackListRepository;
import jaeik.growfarm.repository.user.UserJdbcRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.kakao.KakaoService;
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
    private final KakaoService kakaoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlackListRepository blackListRepository;
    private final EmitterRepository emitterRepository;
    private final TempUserDataManager tempUserDataManager;
    private final AuthUpdateService authUpdateService;
    private final UserJdbcRepository userJdbcRepository;
    private final TokenRepository tokenRepository;

    /**
     * <h3>카카오 로그인</h3>
     *
     * <p>
     * 기존 회원은 Jwt가 담긴 쿠키 쌍을 반환하고, 신규 회원은 UUID가 담긴 임시 쿠키를 반환한다.
     * </p>
     *
     * @param code     프론트에서 반환된 카카오 인가 코드
     * @param fcmToken Firebase Cloud Messaging 토큰
     * @return Jwt가 삽입된 쿠키 또는 토큰 ID
     * @author Jaeik
     * @since 1.0.0
     */
    public LoginResponseDTO<?> processKakaoLogin(String code, String fcmToken) {
        validateLogin();

        TokenDTO tokenDTO = kakaoService.getToken(code);
        KakaoInfoDTO kakaoInfoDTO = kakaoService.getUserInfo(tokenDTO.getKakaoAccessToken());

        Long kakaoId = kakaoInfoDTO.getKakaoId();
        Optional<Users> existingUser = checkExistingUser(kakaoId);

        if (existingUser.isPresent()) {
            return existingUserLogin(existingUser.get(), kakaoInfoDTO, tokenDTO, fcmToken);
        } else {
            if (checkBlackList(kakaoId)) {
                throw new CustomException(ErrorCode.BLACKLIST_USER);
            }
            return newUserLogin(kakaoInfoDTO, tokenDTO, fcmToken);
        }
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

    /**
     * <h3>기존 사용자 조회</h3>
     *
     * <p>
     * 카카오 ID로 기존 사용자를 조회한다.
     * </p>
     *
     * @param kakaoId 카카오 ID
     * @return Optional<Users> 기존 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    private Optional<Users> checkExistingUser(Long kakaoId) {
        return userRepository.findByKakaoId(kakaoId);
    }

    /**
     * <h3>블랙리스트 사용자 확인</h3>
     *
     * <p>
     * 카카오 ID가 블랙리스트에 있는지 확인한다.
     * </p>
     *
     * @param kakaoId 카카오 ID
     * @return boolean 블랙리스트 여부
     * @author Jaeik
     * @since 1.0.0
     */
    private boolean checkBlackList(Long kakaoId) {
        return blackListRepository.existsByKakaoId(kakaoId);
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
    private LoginResponseDTO<List<ResponseCookie>> existingUserLogin(Users user, KakaoInfoDTO kakaoInfoDTO,
            TokenDTO tokenDTO, String fcmToken) {
        List<ResponseCookie> cookies = authUpdateService.saveExistUser(user, kakaoInfoDTO, tokenDTO, fcmToken);
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
    private LoginResponseDTO<ResponseCookie> newUserLogin(KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO,
            String fcmToken) {
        String uuid = tempUserDataManager.saveTempData(kakaoInfoDTO, tokenDTO, fcmToken);
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
        return authUpdateService.saveNewUser(userName, uuid, tempUserData.getKakaoInfoDTO(), tempUserData.getTokenDTO(),
                tempUserData.getFcmToken());
    }

    /**
     * <h3>로그아웃</h3>
     *
     * <p>
     * SSE연결과 ContextHolder를 삭제하고 로그아웃 쿠키를 반한합니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 로그아웃 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    public List<ResponseCookie> logout(CustomUserDetails userDetails) {
        try {
            emitterRepository.deleteAllEmitterByUserId(userDetails.getUserId());
            SecurityContextHolder.clearContext();
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

        kakaoService.unlink(userJdbcRepository.getKakaoAccessToken(userDetails.getTokenId()));
        authUpdateService.performWithdrawProcess(userDetails.getUserId());

        return logout(userDetails);
    }

    /**
     * <h3>카카오 로그아웃</h3>
     *
     * <p>
     * 카카오 서버와 통신하여 카카오 로그아웃을 수행합니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public void kakaoLogout(CustomUserDetails userDetails) {
        tokenRepository.findById(userDetails.getTokenId())
                .ifPresent(token -> kakaoService.logout(token.getKakaoAccessToken()));
    }

    /**
     * <h3>카카오 토큰 갱신</h3>
     *
     * <p>
     * 카카오 액세스 토큰이 만료되었을 때, 리프레시 토큰을 사용하여 새로운 액세스 토큰을 갱신합니다.
     * </p>
     *
     * @param token 현재 로그인한 사용자의 토큰 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void renewalKaKaoToken(Token token) {
        Token managedToken = tokenRepository.findById(token.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FIND_TOKEN));

        TokenDTO tokenDTO = kakaoService.refreshToken(managedToken.getKakaoRefreshToken());
        managedToken.updateKakaoToken(tokenDTO.getKakaoAccessToken(), tokenDTO.getKakaoRefreshToken());

        tokenRepository.save(managedToken);
    }
}
