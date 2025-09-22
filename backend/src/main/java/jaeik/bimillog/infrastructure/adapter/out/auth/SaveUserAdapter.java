package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalTokenCommandPort;
import jaeik.bimillog.domain.user.entity.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자 저장 어댑터</h2>
 * <p>소셜 로그인 후 사용자 데이터 저장 및 업데이트를 담당하는 어댑터입니다.</p>
 * <p>신규 사용자 저장, 기존 사용자 로그인 처리, Token 엔티티 생성/저장, JWT 쿠키 발급</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SaveUserAdapter implements SaveUserPort {

    private final GlobalTokenCommandPort globalTokenCommandPort;
    private final AuthCookieManager authCookieManager;
    private final UserQueryUseCase userQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;
    private final RedisUserDataPort redisUserDataPort;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 회원의 소셜 로그인 시 사용자 정보 업데이트와 JWT 쿠키 발급을 처리합니다.</p>
     * <p>프로필 정보 동기화, 새로운 Token 엔티티 생성/저장, FCM 토큰 등록, JWT 쿠키 발급을 수행합니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken FCM 토큰 (선택적)
     * @return JWT 인증 쿠키 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<ResponseCookie> handleExistingUserLogin(SocialUserProfile userProfile, String fcmToken) {
        User user = userQueryUseCase.findByProviderAndSocialId(userProfile.provider(), userProfile.socialId())
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateUserInfo(userProfile.nickname(), userProfile.profileImageUrl());

        Token token = userProfile.token();
        Token newToken = Token.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .users(user)
                .build();

        Long fcmTokenId = registerFcmTokenIfPresent(user.getId(), fcmToken);

        return authCookieManager.generateJwtCookie(UserDetail.of(user,
                globalTokenCommandPort.save(newToken).getId(),
                fcmTokenId));
    }

    /**
     * <h3>신규 사용자 등록</h3>
     * <p>소셜 로그인 회원가입에서 입력받은 닉네임과 임시 데이터를 사용하여 신규 회원을 등록합니다.</p>
     * <p>User 엔티티와 Setting 생성, Token 엔티티 생성/저장, FCM 토큰 등록, 임시 데이터 삭제, JWT 쿠키 발급을 수행합니다.</p>
     *
     * @param userName 사용자가 입력한 닉네임
     * @param uuid 임시 사용자 식별 UUID
     * @param userProfile 소셜 사용자 프로필 (OAuth 액세스/리프레시 토큰 포함)
     * @param fcmToken FCM 토큰 (선택적)
     * @return JWT 인증 쿠키 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, SocialUserProfile userProfile, String fcmToken) {
        Setting setting = Setting.createSetting();

        User user = userCommandUseCase.saveUser(User.createUser(userProfile.socialId(), userProfile.provider(), userProfile.nickname(), userProfile.profileImageUrl(), userName, setting));

        Long fcmTokenId = registerFcmTokenIfPresent(user.getId(), fcmToken);

        Token token = userProfile.token();
        redisUserDataPort.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(UserDetail.of(user,
                globalTokenCommandPort.save(Token.createToken(token.getAccessToken(), token.getRefreshToken(), user)).getId(),
                fcmTokenId));
    }

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>FCM 토큰이 존재할 경우에만 알림 서비스에 등록합니다.</p>
     * <p>{@link #handleExistingUserLogin}, {@link #saveNewUser} 메서드에서 FCM 토큰 등록을 위해 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param fcmToken FCM 토큰 (빈 문자열이나 null인 경우 무시)
     * @return 저장된 FCM 토큰 ID (토큰이 없거나 빈 값인 경우 null)
     * @author Jaeik
     * @since 2.0.0
     */
    private Long registerFcmTokenIfPresent(Long userId, String fcmToken) {
        if (fcmToken != null && !fcmToken.isEmpty()) {
            return notificationFcmUseCase.registerFcmToken(userId, fcmToken);
        }
        return null;
    }
}