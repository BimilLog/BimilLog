package jaeik.growfarm.service;

import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.global.jwt.JwtTokenProvider;
import jaeik.growfarm.repository.admin.BlackListRepository;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.farm.CropRepository;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CropRepository cropRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;
    private final SettingRepository settingRepository;
    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public Object processKakaoLogin(String code) {
        Token token = tokenRepository.save(userUtil.DTOToToken(kakaoService.getToken(code)));
        KakaoInfoDTO kakaoInfoDTO = kakaoService.getUserInfo(token.getKakaoAccessToken());

        if (blackListRepository.existsByKakaoId(kakaoInfoDTO.getKakaoId())) {
            return "차단된 회원은 가입이 불가능 합니다.";
        }

        try {
            Users user = userRepository.findByKakaoId(kakaoInfoDTO.getKakaoId()); // 기존 유저 판단
            user.updateUserInfo(token, kakaoInfoDTO.getKakaoNickname(), kakaoInfoDTO.getThumbnailImage()); // 프로필 업데이트
            UserDTO userDTO = userUtil.UserToDTO(user);
            String jwtAccessToken = jwtTokenProvider.generateAccessToken(userDTO);
            String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
            token.updateJwtRefreshToken(jwtRefreshToken); // 토큰에 JWT 리프레시 토큰 저장

            return jwtTokenProvider.getResponseCookies(jwtAccessToken, jwtRefreshToken, 86400);
        } catch (NullPointerException e) { // 신규 유저면 NPE 발생
            return token.getId();
        }
    }

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

    @Transactional
    public List<ResponseCookie> logout() throws JSONException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("인증된 사용자가 없습니다.");
        }

        // Principal이 String 타입인지 CustomUserDetails 타입인지 확인
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            throw new RuntimeException("유효하지 않은 인증 정보입니다. Principal 타입: " + principal.getClass().getName());
        }

        UserDTO userDTO = customUserDetails.getUserDTO();
        Users user = userRepository.findById(userDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        Token token = tokenRepository.findById(userDTO.getTokenId())
                .orElseThrow(() -> new RuntimeException("토큰을 찾을 수 없습니다."));

        // 이 사용자의 SSE 구독 삭제
        emitterRepository.deleteAllEmitterByUserId(userDTO.getUserId());
        // 이 사용자의 FCM 토큰 삭제
        fcmTokenRepository.deleteFcmTokenByUserId(userDTO.getUserId());

        kakaoService.logout(token.getKakaoAccessToken());
        user.deleteTokenId();
        userRepository.save(user);
        tokenRepository.delete(token);
        SecurityContextHolder.clearContext();
        return jwtTokenProvider.getLogoutCookies();
    }

    // 회원 탈퇴
    @Transactional
    public List<ResponseCookie> withdraw(CustomUserDetails userDetails) {
        try {
            // userDetails 유효성 검사
            if (userDetails == null || userDetails.getUserDTO() == null) {
                throw new RuntimeException("유효하지 않은 인증 정보입니다.");
            }

            Long userId = userDetails.getUserDTO().getUserId();
            Long tokenId = userDetails.getUserDTO().getTokenId();
            Long settingId = userDetails.getUserDTO().getSettingId();
            Token token = tokenRepository.findById(tokenId)
                    .orElseThrow(() -> new IllegalArgumentException("토큰을 찾을 수 없습니다."));

            Setting setting = settingRepository.findById(settingId)
                    .orElseThrow(() -> new IllegalArgumentException("설정을 찾을 수 없습니다."));

            // 사용자의 농작물 삭제
            cropRepository.deleteCropsByUserId(userId);

            // 신고 내역 삭제
            reportRepository.deleteReportByUserId(userId);

            // 1. 다른 사용자가 이 사용자의 게시글에 누른 좋아요 삭제
            postRepository.deletePostLikesByPostUserIds(userId);

            // 2. 다른 사용자가 이 사용자의 댓글에 누른 좋아요 삭제
            commentRepository.deleteCommentLikesByCommentUserIds(userId);

            // 3. 이 사용자가 다른 게시글에 누른 좋아요 삭제
            postRepository.deletePostLikesByUserId(userId);

            // 4. 이 사용자가 다른 댓글에 누른 좋아요 삭제
            commentRepository.deleteCommentLikesByUserId(userId);

            // 5. 이 사용자의 게시글에 달린 댓글 삭제
            commentRepository.deleteCommentsByPostUserIds(userId);

            // 6. 이 사용자가 작성한 댓글 삭제
            commentRepository.deleteCommentsByUserId(userId);

            // 7. 이 사용자의 게시글 삭제
            postRepository.deletePostsByUserId(userId);

            // 8. 이 사용자의 SSE 구독 삭제
            emitterRepository.deleteAllEmitterByUserId(userId);

            // 9. 이 사용자의 알림 삭제
            notificationRepository.deleteNotificationsByUserId(userId);

            // 10. 이 사용자의 FCM 토큰 삭제
            fcmTokenRepository.deleteFcmTokenByUserId(userId);

            // 10. 카카오 서비스 연결 끊기
            kakaoService.unlink(token.getKakaoAccessToken());

            // 11. 이 사용자의 토큰, 유저 정보 삭제
            userRepository.deleteById(userId);
            tokenRepository.delete(token);
            settingRepository.delete(setting);


            SecurityContextHolder.clearContext();
            return jwtTokenProvider.getLogoutCookies();
        } catch (Exception e) {
            System.err.println("회원 탈퇴 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 현재 인증된 사용자 정보를 반환합니다.
     * JwtFilter에서 이미 토큰 검증 및 갱신을 처리하므로 SecurityContext에서 바로 정보를 가져옵니다.
     */
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserDTO();
        }

        return null;
    }
}
