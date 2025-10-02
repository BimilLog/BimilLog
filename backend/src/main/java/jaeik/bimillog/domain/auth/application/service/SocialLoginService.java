
package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.bimillog.domain.auth.application.port.out.AuthToMemberPort;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.in.GlobalFcmSaveUseCase;
import jaeik.bimillog.domain.global.application.port.out.GlobalAuthTokenSavePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenCommandPort;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.in.auth.web.AuthCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h2>소셜 로그인 서비스</h2>
 * <p>소셜 플랫폼 인증 결과를 조립하여 우리 시스템의 로그인 플로우를 완성합니다.</p>
 * <p>프로필 조회 → 블랙리스트 검증 → 기존/신규 분기 → 토큰 발급까지 단일 트랜잭션으로 처리합니다.</p>
 * <p>기존 회원은 Member 도메인에 프로필/카카오 토큰 갱신을 위임하고, 신규 회원은 Redis에 임시 정보를 저장합니다.</p>
 * <p>Auth/Global 도메인 포트를 이용해 JWT·AuthToken·FCM 토큰을 최종적으로 발급합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final SocialStrategyRegistryPort strategyRegistryPort;
    private final AuthToMemberPort authToMemberPort;
    private final BlacklistPort blacklistPort;
    private final GlobalCookiePort globalCookiePort;
    private final GlobalJwtPort globalJwtPort;
    private final GlobalAuthTokenSavePort globalAuthTokenSavePort;
    private final GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;
    private final GlobalFcmSaveUseCase globalFcmSaveUseCase;

    /**
     * <h3>소셜 플랫폼 로그인 처리</h3>
     * <p>외부 소셜 인증 결과(code)를 받아 우리 시스템의 로그인 결과로 변환합니다.</p>
     * <p>기존 회원이라면 Member 도메인으로 프로필을 갱신하고 AuthToken/FCM/JWT를 발급합니다.</p>
     * <p>신규 회원이라면 Redis에 임시 정보를 저장하고 가입 페이지로 넘길 UUID 임시 쿠키를 발급합니다.</p>
     * <p>{@link AuthCommandController}에서 POST /auth/social 요청 처리 시 호출됩니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param code     OAuth 인가 코드
     * @param fcmToken 푸시용 FCM 토큰 (선택 사항)
     * @return 로그인 완료 시 {@link LoginResult.ExistingUser}, 신규 회원이면 {@link LoginResult.NewUser}
     * @throws AuthCustomException 블랙리스트 사용자 또는 이미 로그인한 사용자
     */
    @Override
    @Transactional
    public LoginResult processSocialLogin(SocialProvider provider, String code, String fcmToken) {
        validateLogin();

        // 1. 전략 포트를 통해 OAuth 인증 수행 및 사용자 정보 조회 (한 번의 호출로 토큰 + 사용자 정보 획득)
        SocialStrategyPort strategy = strategyRegistryPort.getStrategy(provider);
        SocialMemberProfile socialUserProfile = strategy.getSocialToken(code);

        // 2. 소셜 프로파일에 fcm토큰 추가
        socialUserProfile.setFcmToken(fcmToken);

        String socialId = socialUserProfile.getSocialId();
        String kakaoAccessToken = socialUserProfile.getKakaoAccessToken();
        String kakaoRefreshToken = socialUserProfile.getKakaoRefreshToken();
        String nickname = socialUserProfile.getNickname();
        String profileImageUrl = socialUserProfile.getProfileImageUrl();

        // 3. 블랙리스트 사용자 확인
        if (blacklistPort.existsByProviderAndSocialId(provider, socialUserProfile.getSocialId())) {
            throw new AuthCustomException(AuthErrorCode.BLACKLIST_USER);
        }

        // 4. 기존 유저 유무 조회
        Optional<Member> member = authToMemberPort.checkMember(provider, socialId);

        // 5. 기존 유저, 신규 유저에 따라 다른 반환값을 LoginResult에 작성
        if (member.isPresent()) {
            Member existingMember = member.get();

            // 5-1 카카오 토큰 생성
            KakaoToken initialKakaoToken = KakaoToken.createKakaoToken(kakaoAccessToken, kakaoRefreshToken);
            KakaoToken persistedKakaoToken = globalKakaoTokenCommandPort.save(initialKakaoToken);

            // 5-2 멤버 정보 업데이트
            Member updateMember = authToMemberPort.handleExistingMember(existingMember, nickname, profileImageUrl, persistedKakaoToken);

            // 5-3 AuthToken 생성
            AuthToken initialAuthToken = AuthToken.createToken("", updateMember);
            AuthToken persistedAuthToken = globalAuthTokenSavePort.save(initialAuthToken);

            // 5-4 FCM 토큰 생성
            Long fcmTokenId = globalFcmSaveUseCase.registerFcmToken(updateMember, fcmToken);

            // 5-5 MemberDetail 생성
            MemberDetail memberDetail = MemberDetail.ofExisting(updateMember, persistedAuthToken.getId(), fcmTokenId);

            // 5-6 액세스 토큰 및 리프레시 토큰 생성 및 업데이트
            String accessToken = globalJwtPort.generateAccessToken(memberDetail);
            String refreshToken = globalJwtPort.generateRefreshToken(memberDetail);
            globalAuthTokenSavePort.updateJwtRefreshToken(persistedAuthToken.getId(), refreshToken);

            // 5-7 JWT 쿠키 생성 및 반환
            List<ResponseCookie> cookies = globalCookiePort.generateJwtCookie(accessToken, refreshToken);
            return new LoginResult.ExistingUser(cookies);
        } else {
            // 6-1 신규 유저: 임시 쿠키 생성 및 반환
            String uuid = UUID.randomUUID().toString();
            authToMemberPort.handleNewUser(socialUserProfile, uuid);
            ResponseCookie tempCookie = globalCookiePort.createTempCookie(uuid);
            return new LoginResult.NewUser(tempCookie);
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
