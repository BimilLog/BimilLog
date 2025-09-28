package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.auth.application.port.out.TokenCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService implements TokenUseCase {

    private final TokenCommandPort tokenCommandPort;

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     * <p>{@link WithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    public void deleteTokens(Long userId, Long tokenId) {
        tokenCommandPort.deleteTokens(userId, tokenId);
    }
}
