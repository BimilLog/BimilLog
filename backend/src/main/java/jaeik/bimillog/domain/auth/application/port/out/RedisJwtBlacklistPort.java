package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.UserBanService;
import jaeik.bimillog.infrastructure.filter.JwtFilter;

import java.time.Duration;

/**
 * <h2>토큰 블랙리스트 관리 포트</h2>
 * <p>Redis를 활용한 JWT 토큰 블랙리스트 관리를 담당하는 포트입니다.</p>
 * <p>토큰 블랙리스트 등록/조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisJwtBlacklistPort {

    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 토큰 해시값이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>차단된 사용자나 강제 로그아웃된 토큰의 접근을 차단하는데 사용됩니다.</p>
     * <p>{@link UserBanService}에서 JWT 토큰 유효성 검증 시 호출됩니다.</p>
     * <p>{@link JwtFilter}에서 인증 미들웨어 검증 플로우에서 호출됩니다.</p>
     *
     * @param tokenHash 검증할 토큰의 해시값
     * @return 블랙리스트에 등록되어 있으면 true, 정상 토큰이면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isBlacklisted(String tokenHash);

    /**
     * <h3>여러 토큰 해시를 블랙리스트에 일괄 등록</h3>
     * <p>여러 토큰 해시를 한 번에 Redis 블랙리스트에 등록하여 즉시 무효화합니다.</p>
     * <p>사용자 차단이나 회원 탈퇴 시 해당 사용자의 모든 활성 토큰을 한번에 무효화하기 위해 사용됩니다.</p>
     * <p>{@link UserBanService}에서 사용자 모든 토큰 블랙리스트 등록 처리 시 호출됩니다.</p>
     *
     * @param tokenHashes 블랙리스트에 등록할 토큰 해시값 목록
     * @param reason 블랙리스트 등록 사유 (로깅용)
     * @param ttl Redis에서의 데이터 만료 시간
     * @author Jaeik
     * @since 2.0.0
     */
    void blacklistTokenHashes(java.util.List<String> tokenHashes, String reason, Duration ttl);
}