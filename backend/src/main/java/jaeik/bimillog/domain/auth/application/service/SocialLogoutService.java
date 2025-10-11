package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <h2>로그아웃 서비스</h2>
 * <p>사용자의 로그아웃 처리와 소셜 플랫폼 연동 해제를 담당하는 서비스입니다.</p>
 * <p>JWT 토큰 무효화, 소셜 플랫폼 로그아웃, 이벤트 발행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLogoutService implements SocialLogoutUseCase {

    private final GlobalSocialStrategyPort strategyRegistry;
    private final GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;


    /**
     * <h3>소셜 플랫폼 로그아웃</h3>
     * <p>사용자의 소셜 플랫폼 세션을 로그아웃 처리합니다.</p>
     * <p>카카오 토큰을 조회하여 소셜 플랫폼 API를 호출합니다.</p>
     *
     * @param memberId 회원 ID
     * @param provider 소셜 플랫폼 제공자
     * @param authTokenId 인증 토큰 ID (현재 사용되지 않음)
     * @throws Exception 소셜 플랫폼 로그아웃 처리 중 예외 발생 시
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void socialLogout(Long memberId, SocialProvider provider) throws Exception {
        KakaoToken kakaoToken = globalKakaoTokenQueryPort.findByMemberId(memberId)
                .orElseThrow(() -> new AuthCustomException(AuthErrorCode.NOT_FIND_TOKEN));
        SocialPlatformStrategy strategy = strategyRegistry.getStrategy(provider);
        strategy.auth().logout(kakaoToken.getKakaoAccessToken());
    }

    /**
     * <h3>강제 로그아웃</h3>
     * <p>관리자에 의한 강제 로그아웃 처리입니다.</p>
     * <p>현재 미구현 상태이며, 향후 사용자 차단 시 소셜 세션 강제 종료를 위해 사용될 예정입니다.</p>
     *
     * @param socialId 소셜 플랫폼 사용자 ID
     * @param provider 소셜 플랫폼 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void forceLogout(String socialId, SocialProvider provider) {
        SocialPlatformStrategy strategy = strategyRegistry.getStrategy(provider);
        strategy.auth().forceLogout(socialId);
    }
}
