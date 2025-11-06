
package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * <h2>소셜 로그인 서비스</h2>
 * <p>소셜 플랫폼 인증 결과를 조립하여 우리 시스템의 로그인 플로우를 시작합니다.</p>
 * <p>외부 OAuth 호출은 트랜잭션 밖에서 수행하며, 실제 DB 작업 및 토큰 발급은 {@link SocialLoginTransactionalService}로 위임합니다.</p>
 * <p>중복 로그인 검증 → 소셜 전략 실행 → FCM 토큰 설정 → 트랜잭션 서비스 위임 순으로 진행됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final GlobalSocialStrategyPort strategyRegistryPort;
    private final SocialLoginTransactionalService socialLoginTransactionalService;

    /**
     * <h3>소셜 플랫폼 로그인 처리</h3>
     * <p>OAuth 인가 코드를 받아 소셜 플랫폼으로부터 사용자 프로필을 가져오고, 트랜잭션 서비스로 위임하여 로그인을 완료합니다.</p>
     * <p>처리 순서: 중복 로그인 검증 → 소셜 전략 실행 → 사용자 프로필 획득 → FCM 토큰 설정 → 트랜잭션 서비스 위임</p>
     * <p>실제 DB 작업 및 토큰 발급은 {@link SocialLoginTransactionalService#finishLogin}에서 수행됩니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param code     OAuth 인가 코드
     * @param fcmToken 푸시용 FCM 토큰 (선택 사항)
     * @return 기존 회원은 {@link LoginResult.ExistingUser}, 신규 회원은 {@link LoginResult.NewUser}
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        SocialPlatformStrategy strategy = strategyRegistryPort.getStrategy(provider);
        SocialMemberProfile socialUserProfile = strategy.auth().getSocialToken(code);
        socialUserProfile.setFcmToken(fcmToken);

        return socialLoginTransactionalService.finishLogin(provider, socialUserProfile);
    }

    /**
     * <h3>중복 로그인 방지 검증</h3>
     * <p>현재 사용자의 인증 상태를 확인하여 중복 로그인을 방지합니다.</p>
     * <p>이미 인증된 사용자가 다시 소셜 로그인을 시도하는 것을 차단합니다.</p>
     * <p>{@link #processSocialLogin} 메서드 시작 시점에서 보안 검증을 위해 호출됩니다.</p>
     *
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
