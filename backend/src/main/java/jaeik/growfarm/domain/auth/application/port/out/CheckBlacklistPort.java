package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.user.domain.SocialProvider;

/**
 * <h2>블랙리스트 확인 포트</h2>
 * <p>블랙리스트 사용자 확인을 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CheckBlacklistPort {

    /**
     * <h3>블랙리스트 사용자 확인</h3>
     * <p>소셜 ID와 제공자 정보로 블랙리스트에 있는지 확인</p>
     *
     * @param provider 제공자 정보
     * @param socialId 소셜 ID
     * @return 블랙리스트 여부
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);
}