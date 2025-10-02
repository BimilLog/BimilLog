package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthTokenPort;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>토큰 명령 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 토큰 쓰기 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalTokenCommandPort를 구현하여 도메인 간 토큰 쓰기 기능을 통합 제공합니다.</p>
 * <p>TokenRepository를 통해 실제 토큰 데이터를 저장/삭제합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthTokenAdapter implements AuthTokenPort {

    private final AuthTokenRepository authTokenRepository;

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     * <p>{@link WithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void deleteTokens(Long memberId, Long tokenId) {
        if (tokenId != null) {
            authTokenRepository.deleteById(tokenId);
        } else {
            authTokenRepository.deleteAllByMemberId(memberId);
        }
    }

    /**
     * <h3>회원의 모든 토큰 삭제</h3>
     * <p>보안 위협 감지 시 특정 회원의 모든 활성 토큰을 무효화합니다.</p>
     * <p>리프레시 토큰 탈취 또는 재사용 공격 감지 시 사용됩니다.</p>
     *
     * @param memberId 회원 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteAllByMemberId(Long memberId) {
        authTokenRepository.deleteAllByMemberId(memberId);
    }

    /**
     * <h3>토큰 사용 기록</h3>
     * <p>리프레시 토큰이 사용될 때마다 호출되어 사용 이력을 기록합니다.</p>
     * <p>재사용 공격 감지를 위해 사용 횟수를 증가시키고 마지막 사용 시각을 업데이트합니다.</p>
     * <p>트랜잭션 내에서 실행되어 DB에 즉시 반영됩니다.</p>
     *
     * @param tokenId 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void markTokenAsUsed(Long tokenId) {
        AuthToken authToken = authTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("AuthToken not found"));
        authToken.markAsUsed();
    }
}