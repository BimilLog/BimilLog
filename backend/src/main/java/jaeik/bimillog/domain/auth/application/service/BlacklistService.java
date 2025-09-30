package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.BlacklistUseCase;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisJwtBlacklistPort;
import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.domain.auth.entity.JwtToken;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import jaeik.bimillog.infrastructure.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>사용자 계정 차단 서비스</h2>
 * <p>사용자 계정 및 JWT 토큰의 블랙리스트 관리를 담당하는 서비스입니다.</p>
 * <p>JWT 토큰 블랙리스트 검증, 사용자 전체 토큰 차단</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService implements BlacklistUseCase {

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final GlobalJwtPort globalJwtPort;
    private final RedisJwtBlacklistPort redisJwtBlacklistPort;
    private final GlobalTokenQueryPort globalTokenQueryPort;
    private final BlacklistPort blacklistPort;

    /**
     * <h3>JWT 토큰 블랙리스트 검증</h3>
     * <p>제공된 JWT 토큰이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>토큰 해시를 생성하여 Redis에서 블랙리스트 등록 여부를 조회합니다.</p>
     * <p>{@link JwtFilter}에서 모든 인증 요청 시 토큰 유효성 검증을 위해 호출됩니다.</p>
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return 블랙리스트에 등록된 토큰이면 true, 정상 토큰이면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isBlacklisted(String token) {
        try {
            String tokenHash = globalJwtPort.generateTokenHash(token);
            boolean isBlacklisted = redisJwtBlacklistPort.isBlacklisted(tokenHash);

            if (isBlacklisted) {
                log.debug("토큰이 블랙리스트에서 발견됨: hash={}", tokenHash.substring(0, 8) + "...");
            }
            return isBlacklisted;
        } catch (Exception e) {
            log.error("토큰 블랙리스트 상태 확인 실패: error={}", e.getMessage(), e);
            // 예외 발생 시 일반회원으로 간주합니다.
            return false;
        }
    }

    /**
     * <h3>사용자 전체 토큰 블랙리스트 등록</h3>
     * <p>특정 사용자가 보유한 모든 활성 JWT 토큰을 블랙리스트에 등록합니다.</p>
     * <p>사용자 계정 정지, 보안 위반, 강제 로그아웃 처리 시 모든 세션을 무효화합니다.</p>
     * <p>{@link UserWithdrawnEvent}, {@link UserBannedEvent} 이벤트 발생 시 토큰 무효화를 위해 호출됩니다.</p>
     *
     * @param userId 토큰을 차단할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void blacklistAllUserTokens(Long userId) {
        try {
            List<JwtToken> userJwtTokens = globalTokenQueryPort.findAllByUserId(userId);

            if (userJwtTokens.isEmpty()) {
                log.info("사용자 {}의 활성 토큰을 찾을 수 없음", userId);
                return;
            }

            // JWT 리프레시 토큰을 블랙리스트에 추가 (액세스 토큰은 짧은 TTL로 자동 만료)
            List<String> tokenHashes = userJwtTokens.stream()
                    .filter(token -> token.getJwtRefreshToken() != null && !token.getJwtRefreshToken().isEmpty())
                    .map(token -> {
                        try {
                            return globalJwtPort.generateTokenHash(token.getJwtRefreshToken());
                        } catch (Exception e) {
                            log.warn("토큰 ID {}의 해시 생성 실패: {}", token.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!tokenHashes.isEmpty()) {
                redisJwtBlacklistPort.blacklistTokenHashes(tokenHashes, DEFAULT_TTL);
                log.info("사용자 {}의 토큰 {}개가 블랙리스트에 추가됨", userId, tokenHashes.size());
            } else {
                log.warn("사용자 {}에 대해 블랙리스트에 추가할 유효한 토큰 해시가 없음", userId);
            }

        } catch (Exception e) {
            log.error("사용자 {}의 모든 토큰 블랙리스트 등록 실패, 오류={}", userId, e.getMessage(), e);
        }
    }

    /**
     * <h3>사용자 블랙리스트 추가</h3>
     * <p>사용자 ID를 기반으로 사용자를 조회하고 해당 사용자의 소셜 정보로 블랙리스트에 추가합니다.</p>
     *
     * @param userId 블랙리스트에 추가할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void addToBlacklist(Long userId, String socialId, SocialProvider provider) {
        BlackList blackList = BlackList.createBlackList(socialId, provider);
        try {
            blacklistPort.saveBlackList(blackList);
            log.info("사용자 블랙리스트 추가 완료 - userId: {}, socialId: {}, provider: {}",
                    userId, socialId, provider);
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 블랙리스트에 등록된 사용자 - userId: {}, socialId: {}",
                    userId, socialId);
        }
    }




}
