package jaeik.bimillog.infrastructure.adapter.auth.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.UserBanPort;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.BlackListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * <h2>사용자 차단 어댑터</h2>
 * <p>Redis와 JPA를 활용한 토큰 블랙리스트 관리와 사용자 차단 처리를 담당합니다.</p>
 * <p>토큰 블랙리스트 조회/등록, 토큰 해시 생성, 사용자 존재성 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBanAdapter implements UserBanPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BlackListRepository blackListRepository;

    // Redis 키 접두사
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";


    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 JWT 토큰 해시값이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>JWT 토큰 검증 과정에서 해당 토큰이 무효화된 토큰인지 확인하기 위해 인증 미들웨어에서 호출합니다.</p>
     * <p>회원 탈퇴, 계정 정지, 강제 로그아웃으로 블랙리스트에 등록된 토큰의 접근 차단을 위해 인증 검증 플로우에서 호출합니다.</p>
     *
     * @param tokenHash SHA-256으로 해시된 JWT 토큰 값
     * @return boolean 블랙리스트에 등록된 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
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
     * <h3>토큰 해시 리스트를 블랙리스트에 등록</h3>
     * <p>제공된 JWT 토큰 해시 목록을 Redis 블랙리스트에 등록하여 즉시 무효화시킵니다.</p>
     * <p>회원 탈퇴 처리 시 해당 사용자의 모든 토큰을 무효화하기 위해 회원 탈퇴 플로우에서 호출합니다.</p>
     * <p>계정 정지나 보안 사고로 인한 강제 로그아웃 시 사용자 보안 강화를 위해 관리자 기능에서 호출합니다.</p>
     *
     * @param tokenHashes 블랙리스트에 등록할 JWT 토큰 해시 목록
     * @param reason 블랙리스트 등록 사유 (로그용)
     * @param ttl Redis 에서의 만료 시간 (TTL)
     * @author Jaeik
     * @since 2.0.0
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
     * <h3>JWT 토큰 해시값 생성</h3>
     * <p>JWT 토큰을 SHA-256 알고리즘으로 해시하여 블랙리스트 키로 사용할 해시값을 생성합니다.</p>
     * <p>토큰 블랙리스트 등록 전에 원본 JWT 토큰을 안전한 해시값으로 변환하기 위해 블랙리스트 등록 플로우에서 호출합니다.</p>
     * <p>전체 토큰을 Redis에 저장하지 않고 해시값만 저장하여 보안성과 메모리 효율성을 향상시킵니다.</p>
     *
     * @param token 해시할 JWT 토큰 문자열
     * @return String SHA-256 해시값 (16진수 문자열)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }


    /**
     * <h3>소셜 계정 영구 차단 여부 확인</h3>
     * <p>소셜 로그인 시 해당 소셜 계정이 영구 차단된 사용자인지 JPA로 확인합니다.</p>
     * <p>소셜 로그인 인증 단계에서 차단된 사용자의 로그인 시도를 방지하기 위해 소셜 로그인 검증 플로우에서 호출합니다.</p>
     * <p>회원 탈퇴나 계정 정지로 인해 BlackList 테이블에 등록된 소셜 계정의 재가입 방지를 위해 사용됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO, NAVER 등)
     * @param socialId 소셜 로그인 사용자 식별자
     * @return boolean 블랙리스트에 등록된 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
        return blackListRepository.existsByProviderAndSocialId(provider, socialId);
    }


    /**
         * <h3>토큰 블랙리스트 정보 저장용 내부 클래스</h3>
         */
        private record TokenBlacklistInfo(String reason, long timestamp) {
    }
}