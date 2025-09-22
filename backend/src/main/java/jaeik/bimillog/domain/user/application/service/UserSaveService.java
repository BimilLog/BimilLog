package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.user.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserSaveUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSaveService implements UserSaveUseCase {

    private final UserQueryPort userQueryPort;
    private final SaveUserPort saveUserPort;
    private final RedisUserDataPort redisUserDataPort;

    /**
     * <h3>소셜 정보로 사용자 조회</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link UserQueryUseCase}에서 소셜 로그인 사용자 조회 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public LoginResult saveUserData(SocialProvider provider, SocialUserProfile authResult, String fcmToken) {
        Optional<User> existingUser = userQueryPort.findByProviderAndSocialId(provider, authResult.socialId());
        return processUserLogin(fcmToken, existingUser, authResult);
    }

    /**
     * <h3>사용자 로그인 처리</h3>
     * <p>기존 사용자와 신규 사용자를 구분하여 각각의 로그인 처리를 수행합니다.</p>
     * <p>기존 사용자: 프로필 업데이트 후 즉시 로그인 완료</p>
     * <p>신규 사용자: 임시 데이터 저장 후 회원가입 페이지로 안내</p>
     * <p>{@link #}에서 OAuth 인증 완료 후 호출됩니다.</p>
     *
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @param existingUser 기존 사용자 확인 결과
     * @param authResult 소셜 로그인 인증 결과
     * @return LoginResult 기존 사용자(쿠키) 또는 신규 사용자(UUID) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult processUserLogin(String fcmToken, Optional<User> existingUser, SocialUserProfile authResult) {
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            List<ResponseCookie> cookies = saveUserPort.handleExistingUserLogin(user, authResult, fcmToken);
            return new LoginResult.ExistingUser(cookies);
        } else {
            return handleNewUser(authResult, fcmToken);
        }
    }

    /**
     * <h3>신규 사용자 임시 데이터 저장</h3>
     * <p>최초 소셜 로그인하는 사용자의 임시 정보를 저장합니다.</p>
     * <p>회원가입 페이지에서 사용할 UUID 키와 임시 쿠키를 생성합니다.</p>
     * <p>{@link #}에서 신규 사용자 판별 후 호출됩니다.</p>
     *
     * @param authResult 소셜 로그인 인증 결과
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @return NewUser 회원가입용 UUID와 임시 쿠키 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult.NewUser handleNewUser(SocialUserProfile authResult, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        redisUserDataPort.saveTempData(uuid, authResult, fcmToken);
        ResponseCookie tempCookie = redisUserDataPort.createTempCookie(uuid);
        return new LoginResult.NewUser(uuid, tempCookie);
    }


}
