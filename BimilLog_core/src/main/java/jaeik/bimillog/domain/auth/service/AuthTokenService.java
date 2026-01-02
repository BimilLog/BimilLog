package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.out.AuthTokenRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>인증 토큰 서비스</h2>
 * <p>authToken의 관리를 담당하는 서비스입니다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final AuthTokenRepository authTokenRepository;

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     */
    public void deleteTokens(Long memberId, Long tokenId) {
        if (tokenId != null) {
            authTokenRepository.deleteById(tokenId);
        } else {
            authTokenRepository.deleteAllByMemberId(memberId);
        }
    }

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>클라이언트에서 전송한 FCM 토큰을 해당 기기의 AuthToken에 등록.</p>
     * <p>FCM 토큰은 기기별 세션 정보(AuthToken)의 일부로 저장됩니다.</p>
     *
     * @param authTokenId 기기별 세션 ID (JWT에 포함된 authTokenId)
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     */
    @Transactional
    public void registerFcmToken(Long authTokenId, String fcmToken) {
        AuthToken authToken = authTokenRepository.findById(authTokenId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_FCM_TOKEN_NOT_FOUND));
        authToken.updateFcmToken(fcmToken);
    }

    /**
     * <h3>AuthToken 저장</h3>
     * <p>회원 가입시 호출 됨.</p>
     */
    @Transactional
    public AuthToken save(AuthToken authToken) {
        return authTokenRepository.save(authToken);
    }
}
