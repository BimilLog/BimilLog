package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import jaeik.bimillog.domain.auth.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import jaeik.bimillog.infrastructure.auth.JwtFilter;
import jaeik.bimillog.domain.auth.application.port.out.UserBanPort;
import jaeik.bimillog.domain.auth.application.port.out.AuthToTokenPort;
import jaeik.bimillog.domain.user.entity.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
public class UserBanService implements UserBanUseCase {

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final UserBanPort userBanPort;
    private final AuthToTokenPort authToTokenPort;

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
            String tokenHash = userBanPort.generateTokenHash(token);
            boolean isBlacklisted = userBanPort.isBlacklisted(tokenHash);

            if (isBlacklisted) {
                log.debug("토큰이 블랙리스트에서 발견됨: hash={}", tokenHash.substring(0, 8) + "...");
            }
            return isBlacklisted;
        } catch (Exception e) {
            log.error("토큰 블랙리스트 상태 확인 실패: error={}", e.getMessage(), e);
            // 예외 발생 시 안전하게 블랙리스트로 간주하여 접근을 막습니다.
            return true;
        }
    }

    /**
     * <h3>사용자 전체 토큰 블랙리스트 등록</h3>
     * <p>특정 사용자가 보유한 모든 활성 JWT 토큰을 블랙리스트에 등록합니다.</p>
     * <p>사용자 계정 정지, 보안 위반, 강제 로그아웃 처리 시 모든 세션을 무효화합니다.</p>
     * <p>{@link UserWithdrawnEvent}, {@link AdminWithdrawEvent} 이벤트 발생 시 토큰 무효화를 위해 호출됩니다.</p>
     *
     * @param userId 토큰을 차단할 사용자 ID
     * @param reason 블랙리스트 등록 사유 (로깅용)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void blacklistAllUserTokens(Long userId, String reason) {
        try {
            List<Token> userTokens = authToTokenPort.findAllByUserId(userId);

            if (userTokens.isEmpty()) {
                log.info("사용자 {}의 활성 토큰을 찾을 수 없음", userId);
                return;
            }

            List<String> tokenHashes = userTokens.stream()
                    .map(token -> {
                        try {
                            return userBanPort.generateTokenHash(token.getAccessToken());
                        } catch (Exception e) {
                            log.warn("토큰 ID {}의 해시 생성 실패: {}", token.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!tokenHashes.isEmpty()) {
                userBanPort.blacklistTokenHashes(tokenHashes, reason, DEFAULT_TTL);
                log.info("사용자 {}의 토큰 {}개가 블랙리스트에 추가됨: 사유={}", userId, tokenHashes.size(), reason);
            } else {
                log.warn("사용자 {}에 대해 블랙리스트에 추가할 유효한 토큰 해시가 없음", userId);
            }

        } catch (Exception e) {
            log.error("사용자 {}의 모든 토큰 블랙리스트 등록 실패: 사유={}, 오류={}", userId, reason, e.getMessage(), e);
        }
    }
}
