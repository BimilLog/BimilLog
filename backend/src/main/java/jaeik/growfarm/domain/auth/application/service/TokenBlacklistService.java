package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.TokenBlacklistUseCase;
import jaeik.growfarm.domain.auth.application.port.out.TokenBlacklistCachePort;
import jaeik.growfarm.infrastructure.auth.JwtHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * <h2>토큰 블랙리스트 서비스</h2>
 * <p>JWT 토큰 블랙리스트 관리를 위한 애플리케이션 서비스</p>
 * <p>Redis를 사용하여 고성능 토큰 블랙리스트 기능을 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService implements TokenBlacklistUseCase {

    private final TokenBlacklistCachePort tokenBlacklistCachePort;
    private final JwtHandler jwtHandler;

    /**
     * <h3>토큰을 블랙리스트에 등록</h3>
     * <p>JWT 토큰을 Redis 블랙리스트에 등록합니다.</p>
     * <p>토큰의 만료 시간까지만 블랙리스트에 유지됩니다.</p>
     *
     * @param token JWT 토큰
     * @param reason 블랙리스트 등록 사유
     * @return 등록 성공 여부
     */
    @Override
    public boolean addToBlacklist(String token, String reason) {
        try {
            // 토큰 유효성 검사
            if (!jwtHandler.validateToken(token)) {
                log.warn("Invalid token attempted to be blacklisted: {}", reason);
                return false;
            }

            // 토큰 해시 생성
            String tokenHash = jwtHandler.generateTokenHash(token);
            
            // 토큰 만료 시간 확인
            Date expiration = jwtHandler.getTokenExpiration(token);
            if (expiration == null) {
                log.warn("Could not extract expiration from token for blacklisting");
                return false;
            }

            // 현재 시간과 만료 시간 차이 계산
            Duration ttl = Duration.between(Instant.now(), expiration.toInstant());
            
            // 이미 만료된 토큰은 블랙리스트에 추가할 필요 없음
            if (ttl.isNegative() || ttl.isZero()) {
                log.info("Token already expired, skipping blacklist: {}", reason);
                return true;
            }

            // Redis 블랙리스트에 등록
            tokenBlacklistCachePort.addToBlacklist(tokenHash, reason, ttl);
            
            log.info("Token successfully added to blacklist: reason={}, ttl={}s", reason, ttl.getSeconds());
            return true;

        } catch (Exception e) {
            log.error("Failed to add token to blacklist: reason={}, error={}", reason, e.getMessage(), e);
            return false;
        }
    }

    /**
     * <h3>토큰 블랙리스트 여부 확인</h3>
     * <p>JWT 토큰이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     *
     * @param token JWT 토큰
     * @return 블랙리스트에 등록되어 있으면 true, 아니면 false
     */
    @Override
    public boolean isBlacklisted(String token) {
        try {
            // 토큰 해시 생성
            String tokenHash = jwtHandler.generateTokenHash(token);
            
            // Redis에서 블랙리스트 여부 확인
            boolean isBlacklisted = tokenBlacklistCachePort.isBlacklisted(tokenHash);
            
            if (isBlacklisted) {
                log.debug("Token found in blacklist: hash={}", tokenHash.substring(0, 8) + "...");
            }
            
            return isBlacklisted;

        } catch (Exception e) {
            log.error("Failed to check token blacklist status: error={}", e.getMessage(), e);
            // 에러 발생 시 안전하게 블랙리스트로 간주
            return true;
        }
    }

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 활성 토큰을 블랙리스트에 등록합니다.</p>
     * <p>보안 위반이나 강제 로그아웃 시 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param reason 블랙리스트 등록 사유
     */
    @Override
    public void blacklistAllUserTokens(Long userId, String reason) {
        try {
            // 일반적인 JWT 만료 시간(1시간)을 기본값으로 설정
            Duration defaultTtl = Duration.ofHours(1);
            
            tokenBlacklistCachePort.blacklistAllUserTokens(userId, reason, defaultTtl);
            
            log.info("All tokens for user {} added to blacklist: reason={}", userId, reason);

        } catch (Exception e) {
            log.error("Failed to blacklist all tokens for user {}: reason={}, error={}", 
                    userId, reason, e.getMessage(), e);
        }
    }
}