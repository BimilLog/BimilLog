package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.user.entity.SocialProvider;

import java.time.Duration;

/**
 * <h2>유저 제재 / 강제 탈퇴 관련 포트</h2>
 * <p>Redis를 사용한 토큰 블랙리스트 캐시 처리를 추상화</p>
 * <p>유저 제재 / 강제 탈퇴 관련 작업 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserBanPort {

    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 토큰 해시값이 존재하는지 확인합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     * @return 블랙리스트에 존재하면 true, 아니면 false
     */
    boolean isBlacklisted(String tokenHash);

    /**
     * <h3>여러 토큰 해시를 블랙리스트에 일괄 등록</h3>
     * <p>여러 토큰 해시를 한 번에 블랙리스트에 등록합니다.</p>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 토큰을 무효화하기 위해 사용됩니다.</p>
     *
     * @param tokenHashes 블랙리스트에 등록할 토큰 해시 목록
     * @param reason 블랙리스트 등록 사유
     * @param ttl 만료 시간
     */
    void blacklistTokenHashes(java.util.List<String> tokenHashes, String reason, Duration ttl);

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>주어진 토큰 문자열로부터 해시값을 생성합니다.</p>
     *
     * @param token JWT 토큰 문자열
     * @return 토큰 해시값
     * @since 2.0.0
     * @author Jaeik
     */
    String generateTokenHash(String token);

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 존재 여부 확인</h3>
     * <p>특정 소셜 제공자와 소셜 ID에 해당하는 사용자가 존재하는지 확인합니다.</p>
     *
     * @param provider 소셜 제공자 (예: GOOGLE, FACEBOOK)
     * @param socialId 소셜 ID
     * @return 사용자가 존재하면 true, 아니면 false
     * @since 2.0.0
     * @author Jaeik
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);
}