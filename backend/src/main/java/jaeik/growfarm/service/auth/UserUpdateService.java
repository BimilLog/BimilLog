package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserJdbcRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>UserUpdateService 클래스</h2>
 * <p>사용자 정보를 저장하고 업데이트하는 클래스</p>
 * <p>기존 유저 저장</p>
 * <p>신규 유저 저장</p>
 * @since 1.0.0
 * @author Jaeik
 */
@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TempUserDataManager tempUserDataManager;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserJdbcRepository userJdbcRepository;
    private final CommentRepository commentRepository;

    /**
     * <h3>기존 유저 저장</h3>
     *
     * <p>기존 유저의 정보를 업데이트하고 JWT 쿠키를 생성하여 반환합니다.</p>
     *
     * @param user 기존 유저 정보
     * @param kakaoInfoDTO 카카오 정보 DTO
     * @param tokenDTO 토큰 DTO
     * @param fcmToken FCM 토큰 (선택)
     * @return JWT 쿠키 리스트
     * @since 1.0.0
     * @author Jaeik
     */
    @Transactional
    public List<ResponseCookie> saveExistUser(Users user, KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO, String fcmToken) {
        user.updateUserInfo(kakaoInfoDTO.getKakaoNickname(), kakaoInfoDTO.getThumbnailImage());
        return jwtTokenProvider.generateJwtCookie(new ClientDTO(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                fcmToken != null ? fcmTokenRepository.save(FcmToken.create(user, fcmToken)).getId() : null));
    }

    /**
     * <h3>신규 유저 저장</h3>
     *
     * <p>신규 유저의 정보를 저장하고 JWT 쿠키를 생성하여 반환합니다.</p>
     *
     * @param userName 닉네임
     * @param uuid UUID
     * @param kakaoInfoDTO 카카오 정보 DTO
     * @param tokenDTO 토큰 DTO
     * @param fcmToken FCM 토큰 (선택)
     * @return JWT 쿠키 리스트
     * @since 1.0.0
     * @author Jaeik
     */
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, KakaoInfoDTO kakaoInfoDTO, TokenDTO tokenDTO, String fcmToken) {
        Users user = userRepository.save(Users.createUser(kakaoInfoDTO, userName, settingRepository.save(Setting.createSetting())));
        tempUserDataManager.removeTempData(uuid);
        return jwtTokenProvider.generateJwtCookie(new ClientDTO(user,
                tokenRepository.save(Token.createToken(tokenDTO, user)).getId(),
                fcmToken != null ? fcmTokenRepository.save(FcmToken.create(user, fcmToken)).getId() : null));
    }

    /**
     * <h3>로그아웃 처리</h3>
     *
     * <p>사용자의 모든 토큰과 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 1.0.0
     * @author Jaeik
     */
    @Transactional
    public void logoutUser(Long userId) {
        userJdbcRepository.deleteAllTokensByUserId(userId);
        fcmTokenRepository.deleteByUserId(userId);
    }

    /**
     * <h3>회원 탈퇴 처리</h3>
     *
     * <p>사용자의 댓글을 처리하고, 사용자 정보를 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 1.0.0
     * @author Jaeik
     */
    @Transactional
    public void performWithdrawProcess(Long userId) {
        commentRepository.processUserCommentsOnWithdrawal(userId);
        userRepository.deleteById(userId);
    }
}


