
package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialUseCase;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.auth.application.port.out.UserBanPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.auth.in.web.AuthCommandController;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.KakaoLoginStrategyAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class SocialService implements SocialUseCase {

    private final Map<SocialProvider, SocialLoginStrategyPort> strategies;
    private final SocialPort socialPort;
    private final SaveUserPort saveUserPort;
    private final RedisUserDataPort redisUserDataPort;
    private final UserBanPort userBanPort;

    public SocialService(List<SocialLoginStrategyPort> strategyList,
                        SocialPort socialPort,
                        SaveUserPort saveUserPort,
                        RedisUserDataPort redisUserDataPort,
                        UserBanPort userBanPort) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                strategy -> {
                    // KakaoSocialLoginStrategy는 KAKAO 제공자만 지원
                    if (strategy instanceof KakaoLoginStrategyAdapter) {
                        return SocialProvider.KAKAO;
                    }
                    throw new IllegalArgumentException("지원하지 않는 전략 구현체: " + strategy.getClass().getSimpleName());
                },
                Function.identity(),
                (existing, replacement) -> existing,
                () -> new EnumMap<>(SocialProvider.class)
            ));
        this.socialPort = socialPort;
        this.saveUserPort = saveUserPort;
        this.redisUserDataPort = redisUserDataPort;
        this.userBanPort = userBanPort;
    }

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
        SocialLoginStrategyPort strategy = getStrategy(provider);
        SocialLoginStrategyPort.StrategyLoginResult authResult = 
            strategy.authenticate(provider, code);
        
        LoginResult.SocialUserProfile userProfile = authResult.userProfile();

        // 2. 블랙리스트 사용자 확인
        if (userBanPort.existsByProviderAndSocialId(provider, userProfile.socialId())) {
            throw new AuthCustomException(AuthErrorCode.BLACKLIST_USER);
        }

        // 3. 기존 사용자 확인
        Optional<User> existingUser = socialPort.findExistingUser(provider, userProfile.socialId());

        if (existingUser.isPresent()) {
            // 4a. 기존 사용자 처리: 프로필 업데이트 및 로그인
            socialPort.updateUserProfile(existingUser.get(), userProfile);
            LoginResult.SocialLoginData loginData = new LoginResult.SocialLoginData(
                userProfile, authResult.token(), false
            );
            return handleExistingUser(loginData, fcmToken);
        } else {
            // 4b. 신규 사용자 처리: 임시 데이터 저장
            LoginResult.SocialLoginData loginData = new LoginResult.SocialLoginData(
                userProfile, authResult.token(), true
            );
            return handleNewUser(loginData, fcmToken);
        }
    }

    /**
     * <h3>기존 사용자 로그인 후처리</h3>
     * <p>이미 회원가입된 사용자의 소셜 로그인을 완료 처리합니다.</p>
     * <p>JWT 토큰 생성 및 쿠키 설정을 통해 인증 상태를 확립합니다.</p>
     * <p>{@link #processSocialLogin}에서 기존 사용자 판별 후 호출됩니다.</p>
     *
     * @param loginResult 소셜 플랫폼에서 받은 로그인 결과
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @return ExistingUser JWT 토큰이 포함된 쿠키 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult.ExistingUser handleExistingUser(LoginResult.SocialLoginData loginResult, String fcmToken) {
        List<ResponseCookie> cookies = saveUserPort.handleExistingUserLogin(
                loginResult.userProfile(), loginResult.token(), fcmToken
        );
        return new LoginResult.ExistingUser(cookies);
    }

    /**
     * <h3>신규 사용자 임시 데이터 저장</h3>
     * <p>최초 소셜 로그인하는 사용자의 임시 정보를 저장합니다.</p>
     * <p>회원가입 페이지에서 사용할 UUID 키와 임시 쿠키를 생성합니다.</p>
     * <p>{@link #processSocialLogin}에서 신규 사용자 판별 후 호출됩니다.</p>
     *
     * @param loginResult 소셜 플랫폼에서 받은 로그인 결과
     * @param fcmToken 푸시 알림용 FCM 토큰 (선택사항)
     * @return NewUser 회원가입용 UUID와 임시 쿠키 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult.NewUser handleNewUser(LoginResult.SocialLoginData loginResult, String fcmToken) {
        String uuid = UUID.randomUUID().toString();
        redisUserDataPort.saveTempData(uuid, loginResult.userProfile(), loginResult.token(), fcmToken);
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

        SocialLoginStrategyPort strategy = getStrategy(provider);
        strategy.unlink(provider, socialId);

        log.info("소셜 연결 해제 완료 - 제공자: {}, 소셜 ID: {}", provider, socialId);
    }

    /**
     * <h3>제공자별 전략 조회</h3>
     * <p>소셜 제공자에 맞는 로그인 전략 구현체를 반환합니다.</p>
     * <p>지원하지 않는 제공자의 경우 예외를 발생시킵니다.</p>
     * <p>이 서비스의 각 public 메서드에서 전략 선택을 위해 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @return 해당 제공자의 로그인 전략 구현체
     * @throws IllegalArgumentException 지원하지 않는 제공자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private SocialLoginStrategyPort getStrategy(SocialProvider provider) {
        return Optional.ofNullable(strategies.get(provider))
            .orElseThrow(() -> new IllegalArgumentException(
                "지원하지 않는 소셜 제공자: " + provider + 
                ". 지원 제공자: " + strategies.keySet()
            ));
    }
}
