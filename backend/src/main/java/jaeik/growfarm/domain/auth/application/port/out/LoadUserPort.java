package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;

import java.util.Optional;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>사용자 정보 조회를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadUserPort {

    /**
     * <h3>소셜 ID로 사용자 조회</h3>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return 사용자 정보 (없으면 Optional.empty())
     * @since 2.0.0
     * @author Jaeik
     */
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>사용자 ID로 조회</h3>
     *
     * @param userId 사용자 ID
     * @return 사용자 정보 (없으면 Optional.empty())
     * @since 2.0.0
     * @author Jaeik
     */
    Optional<User> findById(Long userId);
}