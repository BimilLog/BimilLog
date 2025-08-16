package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.common.entity.SocialProvider;

/**
 * <h2>블랙리스트 확인 포트</h2>
 * <p>블랙리스트 사용자 확인을 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface BlacklistPort {

    /**
     * <h3>블랙리스트 사용자 확인</h3>
     * <p>소셜 ID와 제공자 정보로 블랙리스트에 있는지 확인</p>
     *
     * @param provider 제공자 정보
     * @param socialId 소셜 ID
     * @return 블랙리스트 여부
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>JWT 토큰을 해시화하여 블랙리스트 저장용 키를 생성합니다.</p>
     *
     * @param token JWT 토큰
     * @return 토큰 해시값
     * @author Jaeik
     * @since 2.0.0
     */
    String generateTokenHash(String token);
}