package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.out.AuthTokenAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2>인증 토큰 서비스</h2>
 * <p>JWT 토큰 삭제 및 관리를 담당하는 서비스입니다.</p>
 * <p>로그아웃 시 특정 토큰 삭제, 회원탈퇴 시 모든 토큰 삭제 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final AuthTokenAdapter authTokenAdapter;

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    public void deleteTokens(Long memberId, Long tokenId) {
        authTokenAdapter.deleteTokens(memberId, tokenId);
    }
}
