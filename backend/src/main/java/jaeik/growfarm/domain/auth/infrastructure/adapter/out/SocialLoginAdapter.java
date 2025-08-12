package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.persistence.TokenRepository;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.strategy.SocialLoginStrategy;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>소셜 로그인 어댑터</h2>
 * <p>SocialLoginPort의 구현체로, 소셜 로그인을 처리하는 어댑터 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SocialLoginAdapter implements SocialLoginPort {

    private final Map<SocialProvider, SocialLoginStrategy> strategies = new EnumMap<>(SocialProvider.class);
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public SocialLoginAdapter(List<SocialLoginStrategy> strategyList, UserRepository userRepository, jaeik.growfarm.domain.auth.infrastructure.adapter.out.persistence.TokenRepository tokenRepository) {
        strategyList.forEach(strategy -> strategies.put(strategy.getProvider(), strategy));
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    /**
     * <h3>소셜 로그인</h3>
     * <p>소셜 로그인 요청을 처리하고, 로그인 결과를 반환</p>
     * <p>기존 사용자 확인 및 정보 업데이트 로직 포함</p>
     *
     * @param provider 소셜 제공자 (예: KAKAO, NAVER 등)
     * @param code     소셜 로그인 인증 코드
     * @return 로그인 결과 DTO (LoginResultDTO.LoginType 포함)
     * @since 2.1.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public LoginResultDTO login(SocialProvider provider, String code) {
        SocialLoginStrategy strategy = strategies.get(provider);
        LoginResultDTO initialLoginResult = strategy.login(code);

        SocialLoginUserData userData = initialLoginResult.getUserData();
        TokenDTO tokenDTO = initialLoginResult.getTokenDTO();

        Optional<User> existingUser = userRepository.findByProviderAndSocialId(provider, userData.socialId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateUserInfo(userData.nickname(), userData.profileImageUrl());

            jaeik.growfarm.domain.user.entity.Token token = tokenRepository.findByUser(user)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
            token.updateToken(tokenDTO.accessToken(), tokenDTO.refreshToken());

            return new LoginResultDTO(userData, tokenDTO, LoginResultDTO.LoginType.EXISTING_USER);
        } else {
            return new LoginResultDTO(userData, tokenDTO, LoginResultDTO.LoginType.NEW_USER);
        }
    }

    @Override
    public void unlink(SocialProvider provider, String socialId) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.unlink(socialId);
    }

    @Override
    public void logout(SocialProvider provider, String accessToken) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.logout(accessToken);
    }
}