
package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.out.SocialStrategyAdapter;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <h2>소셜 로그인 서비스</h2>
 * <p>소셜 플랫폼 인증 결과를 조립하여 우리 시스템의 로그인 플로우를 시작합니다.</p>
 * <p>외부 OAuth 호출은 트랜잭션 밖에서 수행하며, 실제 DB 작업 및 토큰 발급은 {@link SocialLoginTransactionalService}로 위임합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SocialLoginService {
    private final SocialStrategyAdapter socialStrategyAdapter;
    private final SocialLoginTransactionalService socialLoginTransactionalService;

    /**
     * <h3>소셜 플랫폼 로그인 처리</h3>
     * <p>OAuth 인가 코드를 받아 소셜 플랫폼으로부터 사용자 프로필을 가져오고, 트랜잭션 서비스로 위임하여 로그인을 완료합니다.</p>
     * <p>처리 순서: 중복 로그인 검증 → 소셜 전략 실행 → 사용자 프로필 획득 → 트랜잭션 서비스 위임</p>
     * <p>실제 DB 작업 및 토큰 발급은 {@link SocialLoginTransactionalService#finishLogin}에서 수행됩니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (KAKAO, NAVER 등)
     * @param code     OAuth 인가 코드
     * @param state    OAuth state 파라미터 (CSRF 방지용, 일부 제공자에서 필수)
     * @return 기존 회원은 {@link LoginResult.ExistingUser}, 신규 회원은 {@link LoginResult.NewUser}
     */
    public LoginResult processSocialLogin(SocialProvider provider, String code, String state) {
        SocialStrategy strategy = socialStrategyAdapter.getStrategy(provider);
        SocialMemberProfile socialUserProfile = strategy.getSocialToken(code, state);
        return socialLoginTransactionalService.finishLogin(provider, socialUserProfile);
    }
}
