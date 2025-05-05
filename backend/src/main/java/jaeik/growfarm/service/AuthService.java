package jaeik.growfarm.service;

import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.repository.admin.BlackListRepository;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/*
 * AuthService 클래스
 * 인증 관련 서비스 클래스
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

    /*
     * 카카오 로그인 처리
     * @param code 카카오 인가 코드
     * @return ResponseCookie 쿠키 또는 토큰 ID (신규 유저)
     *
     * 카카오 로그인 담당 메서드
     *
     * 1. 인가 코드를 통해 카카오 토큰을 발급받아 토큰 DB에 저장.
     * 2. 카카오 API를 통해 사용자 정보를 가져옴.
     * 3. 블랙리스트에 등록된 사용자면 가입 불가.
     * 4. 가져온 사용자 정보가 기존 DB에 있으면 기존유저로 판단 후 프로필 업데이트 하여 DB 저장.
     * 4-1 . JWT 엑세스 토큰과 리프레시 토큰을 생성하여 쿠키에 담아 반환.
     * 5. 사용자 정보가 DB에 없으면 신규 유저로 판단 토큰 ID를 반환.
     *
     * 수정일 : 2025-05-04
     */
    @Transactional
    public Object processKakaoLogin(String code) {
        Token token = tokenRepository.save(userUtil.DTOToToken(kakaoService.getToken(code)));
        KakaoInfoDTO kakaoInfoDTO = kakaoService.getUserInfo(token.getKakaoAccessToken());

        if (blackListRepository.existsByKakaoId(kakaoInfoDTO.getKakaoId())) {
            return "차단된 회원은 가입이 불가능 합니다.";
        }

        Optional<Users> optionalUsers = userRepository.findByKakaoId(kakaoInfoDTO.getKakaoId());

        if (optionalUsers.isPresent()) {
            Users user = optionalUsers.get();
            user.updateUserInfo(token, kakaoInfoDTO.getKakaoNickname(), kakaoInfoDTO.getThumbnailImage());
            UserDTO userDTO = userUtil.UserToDTO(user);
            String jwtAccessToken = jwtTokenProvider.generateAccessToken(userDTO);
            String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
            token.updateJwtRefreshToken(jwtRefreshToken);
            return jwtTokenProvider.getResponseCookies(jwtAccessToken, jwtRefreshToken, 86400);
        } else {
            return token.getId();
        }
    }


    /*
     * 카카오 로그인 후 자체 서비스 회원가입 처리
     * @param tokenId 카카오 로그인 후 반환된 토큰 ID
     * @param farmName 사용자가 입력한 농장 이름
     * @return ResponseCookie 쿠키
     *
     * 카카오 로그인 후 회원가입 처리 메서드
     *
     * 1. 토큰 ID를 통해 카카오 액세스 토큰을 가져옴.
     * 2. 토큰을 이용해 사용자 정보를 가져옴.
     * 3. 기본 설정값을 설정하여 DB에 저장.
     * 4. 사용자 정보를 DB에 저장.
     * 5. JWT 엑세스 토큰과 리프레시 토큰을 생성하여 쿠키에 담아 반환.
     *
     * 수정일 : 2025-05-03
     */
    @Transactional
    public List<ResponseCookie> signUp(Long tokenId, String farmName) {
        Token token = tokenRepository.findById(tokenId).orElseThrow();
        String kakaoAccessToken = token.getKakaoAccessToken();
        KakaoInfoDTO kakaoInfoDTO = kakaoService.getUserInfo(kakaoAccessToken);

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

    /*
     * 로그아웃 처리
     * @param userDetails 인증된 사용자 정보
     * @return ResponseCookie 쿠키
     *
     * 로그아웃 처리 메서드
     *
     * 1. 인증 정보를 가져옴.
     * 2. 인증 정보에서 사용자 정보를 가져옴.
     * 3. 이 사용자의 SSE 구독 삭제.
     * 4. 이 사용자의 FCM 토큰 삭제.
     * 5. 카카오 서비스 로그아웃.
     * 6. 사용자 정보에서 토큰 ID 삭제.
     * 7. 사용자 정보를 DB에 저장.
     * 8. SecurityContext를 초기화.
     * 9. 기존 쿠키에 토큰을 삭제하여 반환.
     *
     * 수정일 : 2025-05-04
     */
    @Transactional
    public List<ResponseCookie> logout(CustomUserDetails userDetails) throws JSONException {
        if (userDetails == null) {
            throw new RuntimeException("다시 로그인 해 주세요.");
        }

        Long userId = userDetails.getUserId();

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        Token token = tokenRepository.findById(userDetails.getTokenId())
                .orElseThrow(() -> new RuntimeException("토큰을 찾을 수 없습니다. 다시 로그인 해주세요."));

        emitterRepository.deleteAllEmitterByUserId(userId);
        fcmTokenRepository.deleteFcmTokenByUserId(userId);
        kakaoService.logout(token.getKakaoAccessToken());
        user.deleteTokenId();
        userRepository.save(user);
        SecurityContextHolder.clearContext();

        return jwtTokenProvider.getLogoutCookies();
    }

    /*
     * 회원 탈퇴 처리
     * @param userDetails 인증된 사용자 정보
     * @return ResponseCookie 쿠키
     *
     * 회원 탈퇴 처리 메서드
     *
     * 1. 인증 정보를 가져옴.
     * 2. 인증 정보에서 사용자 정보를 가져옴.
     * 3. 사용자의 SSE 구독 삭제.
     * 4. 카카오 서비스 연결 끊기.
     * 5. 유저 정보 삭제.
     * 6. SecurityContext를 초기화.
     * 7. 기존 쿠키에 토큰을 삭제하여 반환.
     *
     * 수정일 : 2025-05-04
     */
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("다시 로그인 해 주세요.");
        }

        Long userId = userDetails.getUserDTO().getUserId();

        Token token = tokenRepository.findById(userDetails.getTokenId())
                .orElseThrow(() -> new IllegalArgumentException("토큰을 찾을 수 없습니다."));


        emitterRepository.deleteAllEmitterByUserId(userId);
        kakaoService.unlink(token.getKakaoAccessToken());
        userRepository.deleteById(userId); // 유저 삭제 시 CasCade로 유저와 연관된 모든 엔티티 삭제

        SecurityContextHolder.clearContext();
        return jwtTokenProvider.getLogoutCookies();
    }
}
