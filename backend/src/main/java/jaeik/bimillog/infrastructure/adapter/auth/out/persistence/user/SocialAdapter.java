package jaeik.bimillog.infrastructure.adapter.auth.out.persistence.user;

import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.SocialLoginStrategy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <h2>소셜 로그인 어댑터</h2>
 * <p>SocialLoginPort의 구현체로, 소셜 로그인을 처리하는 어댑터 클래스</p>
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
     * <h3>소셜 로그인</h3>
     * <p>소셜 로그인 요청을 처리하고, 로그인 결과를 반환</p>
     * <p>기존 사용자 확인 및 정보 업데이트 로직 포함</p>
     * <p>인프라 DTO를 도메인 모델로 변환하여 의존성 역전 원칙 준수</p>
     *
     * @param provider 소셜 제공자 (예: KAKAO, NAVER 등)
     * @param code     소셜 로그인 인증 코드
     * @return 로그인 결과 (isNewUser 포함)
     * @since 2.1.0
     * @author Jaeik
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
     * <p>주어진 소셜 ID에 해당하는 계정의 연결을 해제합니다.</p>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void unlink(SocialProvider provider, String socialId) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.unlink(socialId).subscribe();
    }

    /**
     * <h3>소셜 로그아웃</h3>
     * <p>사용자의 소셜 로그아웃을 처리합니다.</p>
     *
     * @param provider    소셜 제공자
     * @param accessToken 액세스 토큰
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void logout(SocialProvider provider, String accessToken) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.logout(accessToken);
    }
}