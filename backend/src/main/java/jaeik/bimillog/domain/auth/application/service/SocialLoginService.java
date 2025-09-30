
package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.bimillog.domain.auth.application.port.out.AuthToUserPort;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.port.out.TokenCommandPort;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.userdetail.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.userdetail.NewUserDetail;
import jaeik.bimillog.domain.user.entity.userdetail.UserDetail;
import jaeik.bimillog.infrastructure.adapter.in.auth.web.AuthCommandController;
import jaeik.bimillog.domain.auth.entity.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

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
public class SocialLoginService implements SocialLoginUseCase {

    private final SocialStrategyRegistryPort strategyRegistryPort;
    private final AuthToUserPort authToUserPort;
    private final BlacklistPort blacklistPort;
    private final GlobalCookiePort globalCookiePort;
    private final GlobalJwtPort globalJwtPort;
    private final TokenCommandPort tokenCommandPort;

    /**
     * <h3>소셜 플랫폼 로그인 처리</h3>
     * <p>외부 소셜 플랫폼을 통한 사용자 인증 및 로그인을 처리합니다.</p>
     * <p>기존 사용자는 즉시 로그인 처리하고, 신규 사용자는 회원가입을 위한 임시 데이터를 저장합니다.</p>
     * <p>{@link AuthCommandController}에서 소셜 로그인 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (DTO에서 이미 검증됨)
     * @param code     OAuth 인가 코드 (DTO에서 이미 검증됨)
     * @param fcmToken 푸시 알림용 Firebase Cloud Messaging 토큰 (선택사항)
     * @return LoginResult 기존 사용자(쿠키) 또는 신규 사용자(UUID) 정보
     * @throws AuthCustomException 블랙리스트 사용자인 경우
     * @throws AuthCustomException 이미 로그인 상태인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        // 1. 전략 포트를 통해 OAuth 인증 수행
        SocialStrategyPort strategy = strategyRegistryPort.getStrategy(provider);
        KakaoToken kakaoToken = strategy.getToken(code);
        KakaoUserInfo kakaoUserInfo = strategy.getUserInfo(kakaoToken.getKakaoAccessToken());

        // 2. KakaoToken, KakaoUserInfo, fcmToken을 조합하여 SocialUserProfile 생성
        SocialUserProfile socialUserProfile = SocialUserProfile.of(
                kakaoUserInfo.getSocialId(),
                kakaoUserInfo.getEmail(),
                kakaoUserInfo.getProvider(),
                kakaoUserInfo.getNickname(),
                kakaoUserInfo.getProfileImageUrl(),
                kakaoToken.getKakaoAccessToken(),
                kakaoToken.getKakaoRefreshToken(),
                fcmToken
        );

        // 3. 블랙리스트 사용자 확인
        if (blacklistPort.existsByProviderAndSocialId(provider, socialUserProfile.getSocialId())) {
            throw new AuthCustomException(AuthErrorCode.BLACKLIST_USER);
        }

        // 4. 로그인 이후 유저 데이터 작업 유저 도메인으로 책임 위임 결과 값으로 유저 정보 획득
        UserDetail userDetail = authToUserPort.delegateUserData(provider, socialUserProfile);

        // 5. 기존 유저, 신규 유저에 따라 다른 반환값을 LoginResult에 작성
        if (userDetail instanceof ExistingUserDetail existingDetail) {

            // 5-1. JWT 액세스 토큰 및 리프레시 토큰 생성
            String accessToken = globalJwtPort.generateAccessToken(existingDetail);
            String refreshToken = globalJwtPort.generateRefreshToken(existingDetail);

            // 5-2. DB에 JWT 리프레시 토큰 저장 (보안 강화)
            tokenCommandPort.updateJwtRefreshToken(existingDetail.getTokenId(), refreshToken);

            // 5-3. JWT 쿠키 생성 및 반환
            List<ResponseCookie> cookies = globalCookiePort.generateJwtCookie(accessToken, refreshToken);
            return new LoginResult.ExistingUser(cookies);
        } else {
            ResponseCookie tempCookie = globalCookiePort.createTempCookie((NewUserDetail) userDetail);
            return new LoginResult.NewUser(((NewUserDetail) userDetail).getUuid(), tempCookie);
        }
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
