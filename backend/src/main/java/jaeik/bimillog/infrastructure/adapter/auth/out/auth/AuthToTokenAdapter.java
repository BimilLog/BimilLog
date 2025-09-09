package jaeik.bimillog.infrastructure.adapter.auth.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthToTokenPort;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * <h2>인증 토큰 어댑터</h2>
 * <p>
 * 헥사고날 아키텍처의 Secondary Adapter로서 AuthToTokenPort 인터페이스를 구현합니다.
 * </p>
 * <p>
 * JPA Repository를 사용하여 Token 엔티티의 조회 작업을 수행합니다.
 * Auth 도메인에서 토큰 관련 데이터에 접근할 때 사용되는 어댑터입니다.
 * </p>
 * <p>
 * 이 어댑터가 존재하는 이유: Auth 도메인의 토큰 검증 및 사용자 토큰 관리 작업에서
 * User 도메인의 Token 엔티티에 접근해야 하는 크로스 도메인 의존성을 해결하기 위해 분리되었습니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class AuthToTokenAdapter implements AuthToTokenPort {

    private final TokenRepository tokenRepository;

    /**
     * <h3>토큰 ID로 토큰 엔티티 조회</h3>
     * <p>특정 토큰 ID에 해당하는 Token 엔티티를 JPA로 조회합니다.</p>
     * <p>JWT 토큰 검증 과정에서 토큰 정보 확인을 위해 인증 서비스에서 호출합니다.</p>
     * <p>로그인 상태 확인 및 토큰 유효성 검증을 위해 AuthTokenValidationService에서 호출합니다.</p>
     *
     * @param tokenId 조회할 토큰 ID
     * @return Optional<Token> 조회된 토큰 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Token> findById(Long tokenId) {
        return tokenRepository.findById(tokenId);
    }

    /**
     * <h3>사용자의 모든 토큰 목록 조회</h3>
     * <p>특정 사용자가 소유한 모든 Token 엔티티를 JPA로 조회합니다.</p>
     * <p>회원 탈퇴 처리 시 해당 사용자의 모든 토큰을 블랙리스트에 등록하기 위해 회원 탈퇴 플로우에서 호출합니다.</p>
     * <p>사용자 계정 보안 강화를 위해 모든 기기에서 강제 로그아웃 시키는 관리자 기능에서 호출합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return List<Token> 사용자의 모든 토큰 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Token> findAllByUserId(Long userId) {
        return tokenRepository.findByUsersId(userId);
    }
}