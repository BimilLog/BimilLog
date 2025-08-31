package jaeik.bimillog.infrastructure.adapter.auth.out.persistence.user;

import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.blacklist.BlackListRepository;
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

}