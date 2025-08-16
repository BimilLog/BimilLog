package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.auth.application.port.out.BlacklistPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.blacklist.BlackListRepository;
import jaeik.growfarm.infrastructure.auth.JwtHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>블랙리스트 JPA 어댑터</h2>
 * <p>블랙리스트 확인을 위한 JPA 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class BlacklistJpaAdapter implements BlacklistPort {

    private final BlackListRepository blackListRepository;

    /**
     * <h3>소셜 제공자와 소셜 ID로 블랙리스트 확인</h3>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return 블랙리스트에 존재하면 true, 아니면 false
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
        return blackListRepository.existsByProviderAndSocialId(provider, socialId);
    }

    private final JwtHandler jwtHandler;

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>JWT 토큰을 해시화하여 블랙리스트 저장용 키를 생성합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 해시값
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String generateTokenHash(String token) {
        return jwtHandler.generateTokenHash(token);
    }
}