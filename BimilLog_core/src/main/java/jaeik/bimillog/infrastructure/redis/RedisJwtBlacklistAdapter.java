package jaeik.bimillog.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * <h2>토큰 블랙리스트 Redis 어댑터</h2>
 * <p>Redis를 활용한 JWT 토큰 블랙리스트 관리를 담당합니다.</p>
 * <p>토큰 블랙리스트 조회/등록</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJwtBlacklistAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 접두사
    private static final String BLACKLIST_KEY_PREFIX = "TemporaryToken:blacklist:";

    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 JWT 토큰 해시값이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>JWT 토큰 검증 과정에서 해당 토큰이 무효화된 토큰인지 확인하기 위해 인증 미들웨어에서 호출합니다.</p>
     * <p>회원 탈퇴, 계정 정지, 강제 로그아웃으로 블랙리스트에 등록된 토큰의 접근 차단을 위해 인증 검증 플로우에서 호출합니다.</p>
     *
     * @param tokenHash SHA-256으로 해시된 JWT 토큰 값
     * @return 블랙리스트에 등록된 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isBlacklisted(String tokenHash) {
        try {
            String key = BLACKLIST_KEY_PREFIX + tokenHash;

            return redisTemplate.hasKey(key);

        } catch (Exception e) {
            log.error("Redis에서 토큰 블랙리스트 확인 실패: tokenHash={}, error={}",
                    maskTokenHash(tokenHash), e.getMessage(), e);
            // Redis 장애 시 안전하게 블랙리스트로 간주
            return true;
        }
    }

    /**
     * <h3>토큰 해시 리스트를 블랙리스트에 등록</h3>
     * <p>제공된 JWT 토큰 해시 목록을 Redis 블랙리스트에 등록하여 즉시 무효화시킵니다.</p>
     * <p>회원 탈퇴 처리 시 해당 사용자의 모든 토큰을 무효화하기 위해 회원 탈퇴 플로우에서 호출합니다.</p>
     * <p>계정 정지나 보안 사고로 인한 강제 로그아웃 시 사용자 보안 강화를 위해 관리자 기능에서 호출합니다.</p>
     *
     * @param tokenHashes 블랙리스트에 등록할 JWT 토큰 해시 목록
     * @param ttl Redis 에서의 만료 시간 (TTL)
     * @author Jaeik
     * @since 2.0.0
     */
    public void blacklistTokenHashes(List<String> tokenHashes, Duration ttl) {
        try {
            if (tokenHashes == null || tokenHashes.isEmpty()) {
                log.warn("블랙리스트에 등록할 토큰 해시가 제공되지 않음");
                return;
            }

            TokenBlacklistInfo info = new TokenBlacklistInfo(System.currentTimeMillis());

            // 개별 토큰 해시들을 모두 블랙리스트에 등록
            for (String tokenHash : tokenHashes) {
                String key = BLACKLIST_KEY_PREFIX + tokenHash;
                redisTemplate.opsForValue().set(key, info, ttl);
            }

            log.info("Redis에 {} 개의 토큰 해시가 블랙리스트에 추가됨, ttl={}s",
                    tokenHashes.size(), ttl.getSeconds());

        } catch (Exception e) {
            log.error("Redis에서 토큰 해시 블랙리스트 등록 실패: count={}, error={}",
                    tokenHashes != null ? tokenHashes.size() : 0, e.getMessage(), e);
            throw new RuntimeException("Redis TemporaryToken blacklist operation failed", e);
        }
    }

    /**
     * <h3>토큰 블랙리스트 정보 저장용 내부 클래스</h3>
     */
    private record TokenBlacklistInfo(long timestamp) {
    }

    private String maskTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isEmpty()) {
            return "unknown";
        }
        return tokenHash.length() <= 8 ? tokenHash : tokenHash.substring(0, 8) + "...";
    }
}
