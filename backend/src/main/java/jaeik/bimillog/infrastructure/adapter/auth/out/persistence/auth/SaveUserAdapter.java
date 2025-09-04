package jaeik.bimillog.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.TokenVO;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>인증 데이터 어댑터</h2>
 * <p>사용자 데이터 저장, 업데이트, 삭제를 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SaveUserAdapter implements SaveUserPort {

    private final TokenRepository tokenRepository;
    private final AuthCookieManager authCookieManager;
    private final UserQueryUseCase userQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;
    private final RedisUserDataPort redisUserDataPort;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>소셜 사용자 프로필 정보를 업데이트하고, FCM 토큰을 등록합니다.</p>
     *
     * @param userProfile  소셜 사용자 프로필 (순수 도메인 모델)
     * @param tokenVO  토큰 정보
     * @param fcmToken  FCM 토큰 (선택적)
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> handleExistingUserLogin(SocialLoginPort.SocialUserProfile userProfile, TokenVO tokenVO, String fcmToken) { // fcmToken 인자 추가
        User user = userQueryUseCase.findByProviderAndSocialId(userProfile.provider(), userProfile.socialId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        user.updateUserInfo(userProfile.nickname(), userProfile.profileImageUrl());

        Token newToken = Token.builder()
                .accessToken(tokenVO.accessToken())
                .refreshToken(tokenVO.refreshToken())
                .users(user)
                .build();

        registerFcmTokenIfPresent(user.getId(), fcmToken);

        return authCookieManager.generateJwtCookie(UserDTO.of(user,
                tokenRepository.save(newToken).getId(),
                null));
    }

    /**
     * <h3>신규 사용자 정보 저장</h3>
     * <p>임시 UUID를 사용하여 새로운 사용자를 등록하고, FCM 토큰이 존재하면 이벤트를 발행합니다.</p>
     *
     * @param userName  사용자의 이름
     * @param uuid      임시 UUID
     * @param userProfile  소셜 사용자 프로필 (순수 도메인 모델)
     * @param tokenVO  토큰 정보
     * @param fcmToken  FCM 토큰 (선택적)
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialLoginPort.SocialUserProfile userProfile, TokenVO tokenVO, String fcmToken) { // fcmToken 인자 추가
        Setting setting = Setting.createSetting();
        
        User user = userCommandUseCase.save(User.createUser(userProfile, userName, setting));

        registerFcmTokenIfPresent(user.getId(), fcmToken);

        redisUserDataPort.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(UserDTO.of(user,
                tokenRepository.save(Token.createToken(tokenVO.accessToken(), tokenVO.refreshToken(), user)).getId(),
                null));
    }

    /**
     * <h3>FCM 토큰 등록</h3>
     * <p>FCM 토큰이 존재할 경우에만 등록합니다.</p>
     *
     * @param userId 사용자 ID
     * @param fcmToken FCM 토큰 (null 또는 빈 문자열 가능)
     * @since 2.0.0
     * @author Jaeik
     */
    private void registerFcmTokenIfPresent(Long userId, String fcmToken) {
        if (fcmToken != null && !fcmToken.isEmpty()) {
            notificationFcmUseCase.registerFcmToken(userId, fcmToken);
        }
    }
}