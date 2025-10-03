package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
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

    private final SocialStrategyRegistryPort strategyRegistry;
    private final GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;


    @Override
    public void socialLogout(Long memberId, SocialProvider provider, Long authTokenId) throws Exception {
        KakaoToken kakaoToken = globalKakaoTokenQueryPort.findByMemberId(memberId)
                .orElseThrow(() -> new AuthCustomException(AuthErrorCode.NOT_FIND_TOKEN));
        SocialStrategyPort strategy = strategyRegistry.getStrategy(provider);
        strategy.logout(provider, kakaoToken.getKakaoAccessToken());
    }

    @Override
    public void forceLogout(Long memberId, SocialProvider provider, String socialId) {

    }
}
