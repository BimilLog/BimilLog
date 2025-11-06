package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2>카카오 토큰 명령 서비스</h2>
 * <p>카카오 OAuth 토큰 삭제 기능을 제공하는 서비스입니다.</p>
 * <p>로그아웃, 회원탈퇴, 사용자 차단 시 카카오 토큰 정리를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class KakaoTokenService {

    private final GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;

    /**
     * <h3>사용자 ID로 카카오 토큰 삭제</h3>
     * <p>특정 사용자의 카카오 OAuth 토큰을 삭제합니다.</p>
     * <p>로그아웃, 회원탈퇴, 사용자 차단 시 호출되어 카카오 토큰을 정리합니다.</p>
     *
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteByMemberId(Long memberId) {
        globalKakaoTokenCommandPort.deleteByMemberId(memberId);
    }
}