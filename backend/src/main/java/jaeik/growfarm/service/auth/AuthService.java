package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.BlackListRepository;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.KakaoService;
import jaeik.growfarm.util.LoginResponse;
import jaeik.growfarm.util.TempUserDataManager;
import jaeik.growfarm.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
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
 * <p>인증 관련 비즈니스 로직을 처리한다.</p>
 *
 * @since 1.0.0
 * @author Jaeik
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final KakaoService kakaoService;
    private final UserUtil userUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlackListRepository blackListRepository;
    private final EmitterRepository emitterRepository;
    private final SettingRepository settingRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final TempUserDataManager tempUserDataManager;
    private final UserUpdateService userUpdateService;

    /**
     * <h3>카카오 로그인</h3>
     *
     * <p>
     * 기존 회원은 쿠키를 반환하고, 신규 회원은 토큰 ID를 반환한다.
     * </p>
     *
     * @param code 프론트에서 반환된 카카오 인가 코드
     * @return Jwt가 삽입된 쿠키 또는 토큰 ID
     * @author Jaeik
     * @since 1.0.0
     */
    public LoginResponse<?> processKakaoLogin(String code) {
        validateLogin();

        TokenDTO tokenDTO = kakaoService.getToken(code);
        KakaoInfoDTO kakaoInfoDTO = kakaoService.getUserInfo(tokenDTO.getKakaoAccessToken());

        Long kakaoId = kakaoInfoDTO.getKakaoId();
        Optional<Users> existingUser = checkExistingUser(kakaoId);

        if (existingUser.isPresent()) {
            return existingUserLogin(existingUser.get(), kakaoInfoDTO, tokenDTO);
        } else {
            if (checkBlackList(kakaoId)) {
                throw new CustomException(ErrorCode.BLACKLIST_USER);
            }
            return newUserLogin(kakaoInfoDTO, tokenDTO);
        }
    }

    /**
     * <h3>로그인 유효성 검사</h3>
     *
     * <p>현재 로그인 상태인지 확인하고, 이미 로그인 된 경우 예외를 발생시킨다.</p>
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
     * <p>카카오 ID로 기존 사용자를 조회한다.</p>
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
     * <p>카카오 ID가 블랙리스트에 있는지 확인한다.</p>
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
     * <p>기존 사용자의 정보를 업데이트하고 JWT 토큰을 생성하여 쿠키를 반환한다.</p>
     *
     * @param user         기존 사용자 정보
     * @param kakaoInfoDTO 카카오 사용자 정보
     * @param tokenDTO     카카오 토큰 정보
     * @return LoginResponse<List<ResponseCookie>> JWT가 삽입된 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    private LoginResponse<List<ResponseCookie>> existingUserLogin(Users user, KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO) {
        TokenDTO updateTokenDTO = userUpdateService.updateUserInfo(user, kakaoInfoDTO, tokenDTO);
        List<ResponseCookie> cookies = jwtTokenProvider.getResponseCookies(updateTokenDTO.getJwtAccessToken(), (updateTokenDTO.getJwtRefreshToken()));
        return LoginResponse.existingUser(cookies);
    }

    /**
     * <h3>신규 사용자 로그인 처리</h3>
     *
     * <p>신규 사용자의 정보를 TempUserDataManager 메모리에 임시저장하고 UUID를 담은 쿠키를 반환한다.</p>
     *
     * @param kakaoInfoDTO 카카오 사용자 정보
     * @param tokenDTO     카카오 토큰 정보
     * @return LoginResponse<ResponseCookie> UUID가 삽입된 쿠키
     * @author Jaeik
     * @since 1.0.0
     */
    private LoginResponse<ResponseCookie> newUserLogin(KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO) {
        String uuid = tempUserDataManager.saveTempData(kakaoInfoDTO, tokenDTO);
        return LoginResponse.newUser(uuid);
    }


    /**
     * <h3>회원가입 API</h3>
     *
     * <p>
     * 임시 토큰을 통해 사용자 정보를 가져와 회원가입을 처리한다.
     * </p>
     *
     * @param tokenId  임시 토큰 ID
     * @param farmName 사용자가 설정한 농장 이름
     * @return JWT가 삽입된 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<ResponseCookie> signUp(String farmName, String uuid) {

        TempUserDataManager.TempUserData tempUserData = tempUserDataManager.getTempData(uuid);

        if (tempUserData == null) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        }


        Setting setting = Setting.builder()
                .isFarmNotification(true)
                .isCommentNotification(true)
                .isPostFeaturedNotification(true)
                .isCommentFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        Users user = Users.builder()
                .kakaoId(kakaoInfoDTO.getKakaoId())
                .kakaoNickname(kakaoInfoDTO.getKakaoNickname())
                .thumbnailImage(kakaoInfoDTO.getThumbnailImage())
                .farmName(farmName)
                .role(UserRole.USER)
                .token(token)
                .setting(setting)
                .build();

        userRepository.save(user); // 유저 저장

        UserDTO userDTO = userUtil.UserToDTO(user);

        String jwtAccessToken = jwtTokenProvider.generateAccessToken(userDTO);
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
        token.updateJwtRefreshToken(jwtRefreshToken); // 토큰에 JWT 리프레시 토큰 저장

        return jwtTokenProvider.getResponseCookies(jwtAccessToken, jwtRefreshToken, 86400);
    }

    /**
     * <h3>로그아웃 API</h3>
     *
     * <p>
     * 사용자의 토큰을 무효화하고 카카오 로그아웃을 처리한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 로그아웃 쿠키 리스트
     * @throws JSONException JSON 파싱 오류 시 발생
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<ResponseCookie> logout(CustomUserDetails userDetails) throws JSONException {
        try {
            if (userDetails == null) {
                throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
            }

            Long userId = userDetails.getUserId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_MATCH_USER));
            Token token = tokenRepository.findById(userDetails.getTokenId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));

            emitterRepository.deleteAllEmitterByUserId(userId);
            fcmTokenRepository.deleteFcmTokenByUserId(userId);
            kakaoService.logout(token.getKakaoAccessToken());
            user.deleteTokenId();
            userRepository.save(user);
            SecurityContextHolder.clearContext();

            return jwtTokenProvider.getLogoutCookies();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGOUT_FAIL, e);
        }
    }

    /**
     * <h3>회원탈퇴 API</h3>
     *
     * <p>
     * 사용자의 모든 데이터를 삭제하고 카카오 연결을 해제한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 탈퇴 처리 쿠키 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        try {

            if (userDetails == null) {
                throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
            }

            Long userId = userDetails.getUserDTO().getUserId();

            Token token = tokenRepository.findById(userDetails.getTokenId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));

            emitterRepository.deleteAllEmitterByUserId(userId);
            kakaoService.unlink(token.getKakaoAccessToken());
            userRepository.deleteById(userId); // 유저 삭제 시 CasCade로 유저와 연관된 모든 엔티티 삭제

            SecurityContextHolder.clearContext();
            return jwtTokenProvider.getLogoutCookies();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.WITHDRAW_FAIL, e);
        }
    }
}
