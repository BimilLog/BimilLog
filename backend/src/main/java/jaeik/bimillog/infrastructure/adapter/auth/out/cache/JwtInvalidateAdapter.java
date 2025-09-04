package jaeik.bimillog.infrastructure.adapter.auth.out.cache;

import jaeik.bimillog.domain.auth.application.port.out.JwtInvalidatePort;
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
public class JwtInvalidateAdapter implements JwtInvalidatePort {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 접두사
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";


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

            return redisTemplate.hasKey(key);

        } catch (Exception e) {
            log.error("Redis에서 토큰 블랙리스트 확인 실패: tokenHash={}, error={}", 
                    tokenHash.substring(0, 8) + "...", e.getMessage(), e);
            // Redis 장애 시 안전하게 블랙리스트로 간주
            return true;
        }
    }

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 토큰을 블랙리스트에 등록합니다.</p>
     * <p>사용자별 패턴을 사용하여 일괄 처리합니다.</p>
     *
     * @param reason 블랙리스트 등록 사유
     * @param ttl 만료 시간
     */
    @Override
    public void blacklistTokenHashes(java.util.List<String> tokenHashes, String reason, Duration ttl) {
        try {
            if (tokenHashes == null || tokenHashes.isEmpty()) {
                log.warn("블랙리스트에 등록할 토큰 해시가 제공되지 않음");
                return;
            }

            TokenBlacklistInfo info = new TokenBlacklistInfo(reason, System.currentTimeMillis());
            
            // 개별 토큰 해시들을 모두 블랙리스트에 등록
            for (String tokenHash : tokenHashes) {
                String key = BLACKLIST_KEY_PREFIX + tokenHash;
                redisTemplate.opsForValue().set(key, info, ttl);
            }
            
            log.info("Redis에 {} 개의 토큰 해시가 블랙리스트에 추가됨: reason={}, ttl={}s", 
                    tokenHashes.size(), reason, ttl.getSeconds());

        } catch (Exception e) {
            log.error("Redis에서 토큰 해시 블랙리스트 등록 실패: count={}, error={}", 
                    tokenHashes != null ? tokenHashes.size() : 0, e.getMessage(), e);
            throw new RuntimeException("Redis token blacklist operation failed", e);
        }
    }


    /**
         * <h3>토큰 블랙리스트 정보 저장용 내부 클래스</h3>
         */
        private record TokenBlacklistInfo(String reason, long timestamp) {
    }
}