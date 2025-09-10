package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.auth.application.service.UserBanService;
import jaeik.bimillog.domain.user.entity.SocialProvider;

import java.time.Duration;

/**
 * <h2>사용자 제재 포트</h2>
 * <p>사용자 차단과 토큰 블랙리스트 관리를 담당하는 포트입니다.</p>
 * <p>토큰 블랙리스트 등록/조회, 토큰 해시 생성, 사용자 존재성 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserBanPort {

    /**
     * <h3>토큰 해시 블랙리스트 여부 확인</h3>
     * <p>Redis에서 토큰 해시값이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>차단된 사용자나 강제 로그아웃된 토큰의 접근을 차단하는데 사용됩니다.</p>
     * <p>{@link UserBanService}에서 JWT 토큰 유효성 검증 시 호출됩니다.</p>
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

    /**
     * <h3>토큰 해시 생성</h3>
     * <p>주어진 JWT 토큰 문자열로부터 해시값을 생성합니다.</p>
     * <p>토큰 원본을 직접 저장하지 않고 해시값으로 변환하여 보안을 강화합니다.</p>
     * <p>{@link UserBanService}에서 토큰 블랙리스트 등록이나 검증 시 호출됩니다.</p>
     *
     * @param token 해시값으로 변환할 JWT 토큰 문자열
     * @return 생성된 토큰 해시값 (SHA-256 등)
     * @author Jaeik
     * @since 2.0.0
     */
    String generateTokenHash(String token);

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 존재 여부 확인</h3>
     * <p>특정 소셜 제공자와 소셜 ID에 해당하는 사용자가 데이터베이스에 존재하는지 확인합니다.</p>
     * <p>소셜 계정 연결 해제 전에 해당 사용자가 실제로 존재하는지 검증하는데 사용됩니다.</p>
     * <p>{@link SocialService}에서 소셜 계정 연결 해제 요청 처리 시 사용자 존재성 확인을 위해 호출됩니다.</p>
     *
     * @param provider 확인할 소셜 제공자 (KAKAO, GOOGLE 등)
     * @param socialId 확인할 소셜 플랫폼에서의 사용자 고유 ID
     * @return 해당 소셜 계정으로 등록된 사용자가 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);
}