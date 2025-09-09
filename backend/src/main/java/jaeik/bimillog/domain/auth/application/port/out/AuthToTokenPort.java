package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.user.entity.Token;

import java.util.List;
import java.util.Optional;

/**
 * <h2>토큰 조회 포트</h2>
 * <p>
 * Auth 도메인에서 Token 도메인의 토큰 정보를 조회하기 위한 크로스 도메인 포트입니다.
 * </p>
 * <p>사용자 토큰 관리와 블랙리스트 등록을 위해 토큰 엔티티에 접근할 때 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AuthToTokenPort {

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     * <p>특정 ID에 해당하는 토큰 정보를 조회합니다.</p>
     * <p>토큰 유효성 검증이나 토큰 관련 비즈니스 로직 실행에 사용됩니다.</p>
     * <p>AuthService에서 토큰 검증 또는 사용자 인증 처리 시 호출됩니다.</p>
     *
     * @param tokenId 조회할 토큰의 고유 ID
     * @return 조회된 토큰 엔티티 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Token> findById(Long tokenId);

    /**
     * <h3>사용자의 모든 활성 토큰 조회</h3>
     * <p>특정 사용자가 소유한 모든 활성 토큰을 조회합니다.</p>
     * <p>사용자 차단이나 회원 탈퇴 시 모든 토큰을 블랙리스트에 등록하기 위해 사용됩니다.</p>
     * <p>UserBanService에서 사용자의 모든 토큰을 블랙리스트 등록 처리 시 호출됩니다.</p>
     *
     * @param userId 토큰을 조회할 사용자 ID
     * @return 해당 사용자의 모든 활성 토큰 목록 (만료되지 않은 토큰들)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Token> findAllByUserId(Long userId);
}