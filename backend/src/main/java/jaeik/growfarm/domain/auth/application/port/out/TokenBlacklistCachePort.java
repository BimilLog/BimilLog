package jaeik.growfarm.domain.auth.application.port.out;

import java.time.Duration;

/**
 * <h2>토큰 블랙리스트 캐시 포트</h2>
 * <p>Redis를 사용한 토큰 블랙리스트 캐시 처리를 추상화하는 포트</p>
 * <p>헥사고날 아키텍처의 driven port로 사용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface TokenBlacklistCachePort {

    /**
     * <h3>토큰 해시를 블랙리스트에 등록</h3>
     * <p>Redis에 토큰 해시값을 저장하고 TTL을 설정합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     * @param reason 블랙리스트 등록 사유
     * @param ttl 토큰 만료까지의 시간
     */
    void addToBlacklist(String tokenHash, String reason, Duration ttl);

    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 토큰 해시값이 존재하는지 확인합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     * @return 블랙리스트에 존재하면 true, 아니면 false
     */
    boolean isBlacklisted(String tokenHash);

    // TODO : 이 메서드 사용안되는 중
    /**
     * <h3>블랙리스트에서 토큰 제거</h3>
     * <p>Redis에서 토큰 해시값을 삭제합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     */
    void removeFromBlacklist(String tokenHash);

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 토큰을 블랙리스트에 등록합니다.</p>
     * <p>사용자별 패턴을 사용하여 일괄 처리합니다.</p>
     *
     * @param userId 사용자 ID
     * @param reason 블랙리스트 등록 사유
     * @param ttl 만료 시간
     */
    void blacklistAllUserTokens(Long userId, String reason, Duration ttl);
}