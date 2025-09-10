package jaeik.bimillog.infrastructure.adapter.auth.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.user.application.port.out.TokenPort;
import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자 저장 어댑터</h2>
 * <p>소셜 로그인 완료 후 사용자 데이터의 저장, 업데이트 작업을 담당합니다.</p>
 * <p>신규 사용자 저장, 기존 사용자 로그인 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SaveUserAdapter implements SaveUserPort {

    private final TokenPort tokenPort;
    private final AuthCookieManager authCookieManager;
    private final UserQueryPort userQueryPort;
    private final UserCommandPort UserCommandPort;
    private final RedisUserDataPort redisUserDataPort;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>기존 사용자 로그인 처리</h3>
     * <p>기존 회원의 소셜 로그인 시 사용자 정보 업데이트와 JWT 쿠키 발급을 처리합니다.</p>
     * <p>소셜 로그인 인증 성공 후 기존 회원 확인 시 로그인 처리 플로우에서 호출합니다.</p>
     * <p>프로필 이미지와 닉네임을 소셜 계정 최신 정보로 업데이트하고, FCM 토큰이 있으면 알림 서비스에 등록합니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (순수 도메인 모델)
     * @param token 소셜 로그인에서 획득한 토큰 정보
     * @param fcmToken FCM 토큰 (선택적)
     * @return List<ResponseCookie> JWT 인증 쿠키 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<ResponseCookie> handleExistingUserLogin(LoginResult.SocialUserProfile userProfile, Token token, String fcmToken) { // fcmToken 인자 추가
        User user = userQueryPort.findByProviderAndSocialId(userProfile.provider(), userProfile.socialId());

        user.updateUserInfo(userProfile.nickname(), userProfile.profileImageUrl());

        Token newToken = Token.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .users(user)
                .build();

        registerFcmTokenIfPresent(user.getId(), fcmToken);

        return authCookieManager.generateJwtCookie(UserDetail.of(user,
                tokenPort.save(newToken).getId(),
                null));
    }

    /**
     * <h3>신규 사용자 등록 및 저장</h3>
     * <p>소셜 로그인 회원가입 두 번째 단계에서 입력받은 닉네임과 임시 데이터를 사용하여 신규 회원을 등록합니다.</p>
     * <p>회원가입 완료 후 임시 데이터 정리와 JWT 쿠키 발급을 위해 회원가입 성공 플로우에서 호출합니다.</p>
     * <p>사용자 엔티티 생성과 동시에 기본 설정, FCM 토큰 등록, 임시 데이터 정리를 전빈 처리합니다.</p>
     *
     * @param userName 사용자가 입력한 닉네임
     * @param uuid 임시 사용자 식별 UUID
     * @param userProfile 소셜 사용자 프로필 (순수 도메인 모델)
     * @param token 소셜 로그인에서 획득한 토큰 정보
     * @param fcmToken FCM 토큰 (선택적)
     * @return List<ResponseCookie> JWT 인증 쿠키 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<ResponseCookie> saveNewUser(String userName, String uuid, LoginResult.SocialUserProfile userProfile, Token token, String fcmToken) { // fcmToken 인자 추가
        Setting setting = Setting.createSetting();
        
        User user = UserCommandPort.save(User.createUser(userProfile.socialId(), userProfile.provider(), userProfile.nickname(), userProfile.profileImageUrl(), userName, setting));

        registerFcmTokenIfPresent(user.getId(), fcmToken);

        redisUserDataPort.removeTempData(uuid);
        return authCookieManager.generateJwtCookie(UserDetail.of(user,
                tokenPort.save(Token.createToken(token.getAccessToken(), token.getRefreshToken(), user)).getId(),
                null));
    }

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>FCM 토큰이 존재할 경우에만 알림 서비스에 등록합니다.</p>
     * <p>소셜 로그인 과정에서 FCM 토큰이 포함된 경우 푸시 알림 수신을 위해 내부에서 호출합니다.</p>
     *
     * @param userId 사용자 ID
     * @param fcmToken FCM 토큰 (빈 문자열이나 null인 경우 무시)
     * @author Jaeik
     * @since 2.0.0
     */
    private void registerFcmTokenIfPresent(Long userId, String fcmToken) {
        if (fcmToken != null && !fcmToken.isEmpty()) {
            notificationFcmUseCase.registerFcmToken(userId, fcmToken);
        }
    }
}