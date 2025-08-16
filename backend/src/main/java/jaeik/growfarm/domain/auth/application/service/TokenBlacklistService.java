package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.TokenBlacklistUseCase;
import jaeik.growfarm.domain.auth.application.port.out.AuthPort;
import jaeik.growfarm.domain.auth.application.port.out.BlacklistPort;
import jaeik.growfarm.domain.auth.application.port.out.LoadTokenPort;
import jaeik.growfarm.domain.auth.application.port.out.BlacklistCachePort;
import jaeik.growfarm.domain.user.entity.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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

    private final BlacklistCachePort blacklistCachePort;
    private final BlacklistPort blacklistPort;
    private final LoadTokenPort loadTokenPort;
    private final AuthPort authPort;

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
            String tokenHash = authPort.generateTokenHash(token);
            
            // Redis에서 블랙리스트 여부 확인
            boolean isBlacklisted = blacklistCachePort.isBlacklisted(tokenHash);
            
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
            // 사용자의 모든 활성 토큰 조회
            List<Token> userTokens = loadTokenPort.findAllByUserId(userId);
            
            if (userTokens.isEmpty()) {
                log.info("No active tokens found for user {}", userId);
                return;
            }
            
            // 토큰에서 해시값 생성
            List<String> tokenHashes = userTokens.stream()
                    .map(token -> {
                        try {
                            return authPort.generateTokenHash(token.getAccessToken());
                        } catch (Exception e) {
                            log.warn("Failed to generate hash for token {}: {}", token.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(hash -> hash != null)
                    .collect(Collectors.toList());
            
            if (!tokenHashes.isEmpty()) {
                // 일반적인 JWT 만료 시간(1시간)을 기본값으로 설정
                Duration defaultTtl = Duration.ofHours(1);
                
                // 개별 토큰 해시들을 모두 블랙리스트에 등록
                blacklistCachePort.blacklistTokenHashes(tokenHashes, reason, defaultTtl);
                
                log.info("All {} tokens for user {} added to blacklist: reason={}", 
                        tokenHashes.size(), userId, reason);
            } else {
                log.warn("No valid token hashes generated for user {}", userId);
            }

        } catch (Exception e) {
            log.error("Failed to blacklist all tokens for user {}: reason={}, error={}", 
                    userId, reason, e.getMessage(), e);
        }
    }
}