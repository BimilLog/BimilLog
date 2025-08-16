package jaeik.growfarm.infrastructure.adapter.auth.out.cache;

import jaeik.growfarm.domain.auth.application.port.out.TokenBlacklistCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <h2>토큰 블랙리스트 Redis 캐시 어댑터</h2>
 * <p>Redis를 사용한 토큰 블랙리스트 캐시 구현체</p>
 * <p>헥사고날 아키텍처의 driven adapter로 사용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistCacheAdapter implements TokenBlacklistCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 접두사
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String USER_BLACKLIST_KEY_PREFIX = "user:blacklist:";

    /**
     * <h3>토큰 해시를 블랙리스트에 등록</h3>
     * <p>Redis에 토큰 해시값을 저장하고 TTL을 설정합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     * @param reason 블랙리스트 등록 사유
     * @param ttl 토큰 만료까지의 시간
     */
    @Override
    public void addToBlacklist(String tokenHash, String reason, Duration ttl) {
        try {
            String key = BLACKLIST_KEY_PREFIX + tokenHash;
            
            // Redis에 저장할 데이터 (등록 사유와 시간 정보)
            TokenBlacklistInfo info = new TokenBlacklistInfo(reason, System.currentTimeMillis());
            
            // Redis에 저장하고 TTL 설정
            redisTemplate.opsForValue().set(key, info, ttl);
            
            log.debug("Token added to Redis blacklist: key={}, reason={}, ttl={}s", 
                    key.substring(0, Math.min(key.length(), 20)) + "...", reason, ttl.getSeconds());

        } catch (Exception e) {
            log.error("Failed to add token to Redis blacklist: tokenHash={}, error={}", 
                    tokenHash.substring(0, 8) + "...", e.getMessage(), e);
            throw new RuntimeException("Redis blacklist operation failed", e);
        }
    }

    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 토큰 해시값이 존재하는지 확인합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     * @return 블랙리스트에 존재하면 true, 아니면 false
     */
    @Override
    public boolean isBlacklisted(String tokenHash) {
        try {
            String key = BLACKLIST_KEY_PREFIX + tokenHash;
            Boolean exists = redisTemplate.hasKey(key);
            
            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Failed to check token blacklist in Redis: tokenHash={}, error={}", 
                    tokenHash.substring(0, 8) + "...", e.getMessage(), e);
            // Redis 장애 시 안전하게 블랙리스트로 간주
            return true;
        }
    }

    /**
     * <h3>블랙리스트에서 토큰 제거</h3>
     * <p>Redis에서 토큰 해시값을 삭제합니다.</p>
     *
     * @param tokenHash 토큰 해시값
     */
    @Override
    public void removeFromBlacklist(String tokenHash) {
        try {
            String key = BLACKLIST_KEY_PREFIX + tokenHash;
            redisTemplate.delete(key);
            
            log.debug("Token removed from Redis blacklist: key={}", 
                    key.substring(0, Math.min(key.length(), 20)) + "...");

        } catch (Exception e) {
            log.error("Failed to remove token from Redis blacklist: tokenHash={}, error={}", 
                    tokenHash.substring(0, 8) + "...", e.getMessage(), e);
        }
    }

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 토큰을 블랙리스트에 등록합니다.</p>
     * <p>사용자별 패턴을 사용하여 일괄 처리합니다.</p>
     *
     * @param userId 사용자 ID
     * @param reason 블랙리스트 등록 사유
     * @param ttl 만료 시간
     */
    @Override
    public void blacklistAllUserTokens(Long userId, String reason, Duration ttl) {
        try {
            String userKey = USER_BLACKLIST_KEY_PREFIX + userId;
            
            // 사용자별 블랙리스트 마커 저장
            TokenBlacklistInfo info = new TokenBlacklistInfo(reason, System.currentTimeMillis());
            redisTemplate.opsForValue().set(userKey, info, ttl);
            
            log.info("All tokens for user {} marked as blacklisted in Redis: reason={}, ttl={}s", 
                    userId, reason, ttl.getSeconds());

        } catch (Exception e) {
            log.error("Failed to blacklist all user tokens in Redis: userId={}, error={}", 
                    userId, e.getMessage(), e);
            throw new RuntimeException("Redis user blacklist operation failed", e);
        }
    }

    /**
     * <h3>사용자별 블랙리스트 여부 확인</h3>
     * <p>특정 사용자의 모든 토큰이 블랙리스트되었는지 확인합니다.</p>
     *
     * @param userId 사용자 ID
     * @return 사용자가 블랙리스트되었으면 true, 아니면 false
     */
    public boolean isUserBlacklisted(Long userId) {
        try {
            String userKey = USER_BLACKLIST_KEY_PREFIX + userId;
            Boolean exists = redisTemplate.hasKey(userKey);
            
            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Failed to check user blacklist in Redis: userId={}, error={}", 
                    userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * <h3>토큰 블랙리스트 정보 저장용 내부 클래스</h3>
     */
    private static class TokenBlacklistInfo {
        public final String reason;
        public final long timestamp;

        public TokenBlacklistInfo(String reason, long timestamp) {
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}