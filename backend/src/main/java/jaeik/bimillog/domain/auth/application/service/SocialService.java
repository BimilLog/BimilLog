
package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialUseCase;
import jaeik.bimillog.domain.auth.application.port.out.*;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.auth.in.web.AuthCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <h2>소셜 로그인 서비스</h2>
 * <p>소셜 플랫폼을 통한 로그인 및 계정 연동 해제를 처리하는 서비스입니다.</p>
 * <p>소셜 로그인 처리, 기존/신규 사용자 구분, 소셜 계정 연동 해제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SocialService implements SocialUseCase {

    private final SocialStrategyRegistryPort strategyRegistry;
    private final AuthToUserPort authToUserPort;
    private final SaveUserPort saveUserPort;
    private final RedisUserDataPort redisUserDataPort;
    private final UserBanPort userBanPort;

    // TODO 여기에 트랜잭셔널이 있는것에 대해 검토해야 함. 네트워크 작업을 중간에 두고 트랜잭션이 일어나고있음 트랜잭션은 되도록 어댑터에 배치
    //  트랜잭션은 DB작업을 대상으로 하는것 이 메서드는 많은 private메서드를 하위로 두고 있음 그리고 중간에 네트워크 작업도 일어남
    //  그리고 하위 어댑터들에는 트랜잭션이 달려있음 그래서 이 메서드에 트랜잭셔널이 있는게 타당한지 검토 필요
    /**
     * <h3>소셜 플랫폼 로그인 처리</h3>
     * <p>외부 소셜 플랫폼을 통한 사용자 인증 및 로그인을 처리합니다.</p>
     * <p>기존 사용자는 즉시 로그인 처리하고, 신규 사용자는 회원가입을 위한 임시 데이터를 저장합니다.</p>
     * <p>{@link AuthCommandController}에서 소셜 로그인 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (DTO에서 이미 검증됨)
     * @param code OAuth 인가 코드 (DTO에서 이미 검증됨)
     * @param fcmToken 푸시 알림용 Firebase Cloud Messaging 토큰 (선택사항)
     * @return LoginResult 기존 사용자(쿠키) 또는 신규 사용자(UUID) 정보
     * @throws AuthCustomException 블랙리스트 사용자인 경우
     * @throws AuthCustomException 이미 로그인 상태인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        // 1. 전략 포트를 통해 OAuth 인증 수행
        SocialStrategyPort strategy = strategyRegistry.getStrategy(provider);
        SocialAuthData.AuthenticationResult authResult =
            strategy.authenticate(provider, code);
        
        SocialAuthData.SocialUserProfile userProfile = authResult.userProfile();

        // 2. 블랙리스트 사용자 확인
        if (userBanPort.existsByProviderAndSocialId(provider, userProfile.socialId())) {
            throw new AuthCustomException(AuthErrorCode.BLACKLIST_USER);
        }

        // 3. 기존 사용자 확인
        Optional<User> existingUser = authToUserPort.findExistingUser(provider, userProfile.socialId());

        return processUserLogin(fcmToken, existingUser, userProfile, authResult);
    }

    /**
     * <h3>소셜 계정 연동 해제</h3>
     * <p>사용자의 소셜 플랫폼 계정 연동을 해제합니다.</p>
     * <p>소셜 플랫폼 API를 호출하여 앱 연동을 완전히 차단합니다.</p>
     * <p>{@link UserWithdrawnEvent}, {@link AdminWithdrawEvent} 이벤트 발생 시 소셜 계정 정리를 위해 호출됩니다.</p>
     *
     * @param provider 연동 해제할 소셜 플랫폼 제공자
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlinkSocialAccount(SocialProvider provider, String socialId) {
        log.info("소셜 연결 해제 시작 - 제공자: {}, 소셜 ID: {}", provider, socialId);

        SocialStrategyPort strategy = strategyRegistry.getStrategy(provider);
        strategy.unlink(provider, socialId);

        log.info("소셜 연결 해제 완료 - 제공자: {}, 소셜 ID: {}", provider, socialId);
    }

    /**
     * <h3>사용자 로그인 처리</h3>
     * <p>기존 사용자와 신규 사용자를 구분하여 각각의 로그인 처리를 수행합니다.</p>
     * <p>기존 사용자: 프로필 업데이트 후 즉시 로그인 완료</p>
     * <p>신규 사용자: 임시 데이터 저장 후 회원가입 페이지로 안내</p>
     * <p>{@link #processSocialLogin}에서 OAuth 인증 완료 후 호출됩니다.</p>
     *
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @param existingUser 기존 사용자 확인 결과
     * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필
     * @param authResult 소셜 로그인 인증 결과
     * @return LoginResult 기존 사용자(쿠키) 또는 신규 사용자(UUID) 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult processUserLogin(String fcmToken, Optional<User> existingUser, SocialAuthData.SocialUserProfile userProfile, SocialAuthData.AuthenticationResult authResult) {
        if (existingUser.isPresent()) {
            List<ResponseCookie> cookies = saveUserPort.handleExistingUserLogin(userProfile, authResult.token(), fcmToken);
            return new LoginResult.ExistingUser(cookies);
        } else {
            return handleNewUser(userProfile, authResult.token(), fcmToken);
        }
    }

    /**
     * <h3>신규 사용자 임시 데이터 저장</h3>
     * <p>최초 소셜 로그인하는 사용자의 임시 정보를 저장합니다.</p>
     * <p>회원가입 페이지에서 사용할 UUID 키와 임시 쿠키를 생성합니다.</p>
     * <p>{@link #processSocialLogin}에서 신규 사용자 판별 후 호출됩니다.</p>
     *
     * @param userProfile 소셜 플랫폼에서 받은 사용자 프로필
     * @param token 소셜 로그인으로 발급받은 토큰 정보
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @return NewUser 회원가입용 UUID와 임시 쿠키 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult.NewUser handleNewUser(SocialAuthData.SocialUserProfile userProfile, Token token, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        redisUserDataPort.saveTempData(uuid, userProfile, token, fcmToken);
        ResponseCookie tempCookie = redisUserDataPort.createTempCookie(uuid);
        return new LoginResult.NewUser(uuid, tempCookie);
    }

    /**
     * <h3>중복 로그인 방지 검증</h3>
     * <p>현재 사용자의 인증 상태를 확인하여 중복 로그인을 방지합니다.</p>
     * <p>이미 인증된 사용자가 다시 소셜 로그인을 시도하는 것을 차단합니다.</p>
     * <p>{@link #processSocialLogin} 메서드 시작 시점에서 보안 검증을 위해 호출됩니다.</p>
     *
     * @throws AuthCustomException 이미 로그인 상태인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            throw new AuthCustomException(AuthErrorCode.ALREADY_LOGIN);
        }
    }
}
