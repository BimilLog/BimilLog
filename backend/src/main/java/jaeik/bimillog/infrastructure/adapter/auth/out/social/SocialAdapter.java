package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <h2>소셜 로그인 어댑터</h2>
 * <p>Strategy 패턴을 활용하여 다양한 소셜 로그인 제공자를 통합 처리하는 어댑터입니다.</p>
 * <p>소셜 로그인 처리, 소셜 계정 연동 해제, 소셜 로그아웃</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class SocialAdapter implements SocialPort {

    private final Map<SocialProvider, SocialLoginStrategy> strategies = new EnumMap<>(SocialProvider.class);
    private final UserQueryUseCase userQueryUseCase;

    public SocialAdapter(List<SocialLoginStrategy> strategyList, UserQueryUseCase userQueryUseCase) {
        strategyList.forEach(strategy -> strategies.put(strategy.getProvider(), strategy));
        this.userQueryUseCase = userQueryUseCase;
    }

    /**
     * <h3>소셜 로그인 처리</h3>
     * <p>제공자별 전략을 사용하여 소셜 로그인 인증과 기존 사용자 확인을 처리합니다.</p>
     * <p>Strategy 선택 → 소셜 인증 수행 → 기존 사용자 확인 → 필요시 정보 업데이트 순으로 처리합니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (예: KAKAO 등)
     * @param code 소셜 로그인 OAuth 2.0 인증 코드
     * @return 소셜 로그인 결과 (신규/기존 사용자 구분 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public LoginResult.SocialLoginData login(SocialProvider provider, String code) {
        SocialLoginStrategy strategy = strategies.get(provider);
        SocialLoginStrategy.StrategyLoginResult initialResult = strategy.login(code).block(); // 동기 변환

        LoginResult.SocialUserProfile userProfile = initialResult.userProfile();
        Token token = initialResult.token();

        // 기존 사용자 확인
        try {
            User user = userQueryUseCase.findByProviderAndSocialId(provider, userProfile.socialId());
            
            // 조건부 사용자 정보 업데이트: 변경된 정보가 있을 때만 업데이트
            boolean needsUpdate = !Objects.equals(user.getSocialNickname(), userProfile.nickname());
            if (!Objects.equals(user.getThumbnailImage(), userProfile.profileImageUrl())) {
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                user.updateUserInfo(userProfile.nickname(), userProfile.profileImageUrl());
            }
            return new LoginResult.SocialLoginData(userProfile, token, false); // 기존 사용자
        } catch (UserCustomException e) {
            // 사용자가 존재하지 않는 경우
            return new LoginResult.SocialLoginData(userProfile, token, true); // 신규 사용자
        }
    }

    /**
     * <h3>소셜 계정 연결 해제</h3>
     * <p>제공자별 전략을 사용하여 해당 소셜 계정과의 연결을 해제합니다.</p>
     * <p>각 제공자별 전략에 따라 관리자 API 또는 사용자 API를 사용하여 연결 해제를 수행합니다.</p>
     *
     * @param provider 연결 해제할 소셜 로그인 제공자
     * @param socialId 연결 해제할 소셜 사용자 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void unlink(SocialProvider provider, String socialId) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.unlink(socialId).subscribe();
    }

    /**
     * <h3>소셜 로그아웃</h3>
     * <p>제공자별 전략을 사용하여 해당 소셜 계정에서 로그아웃 처리를 수행합니다.</p>
     * <p>각 제공자별 전략에 따라 소셜 계정 세션 종료 요청을 전송합니다.</p>
     *
     * @param provider 로그아웃할 소셜 로그인 제공자
     * @param accessToken 소셜 로그인 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(SocialProvider provider, String accessToken) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.logout(accessToken);
    }
}