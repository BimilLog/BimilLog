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
 * <p>JWT 토큰 삭제 및 관리를 담당하는 서비스입니다.</p>
 * <p>로그아웃 시 특정 토큰 삭제, 회원탈퇴 시 모든 토큰 삭제 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
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
     * @since 2.0.0
     * @author Jaeik
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
     * <p>클라이언트에서 전송한 FCM 토큰을 해당 기기의 AuthToken에 등록하여 푸시 알림 수신을 준비합니다.</p>
     * <p>FCM 토큰은 기기별 세션 정보(AuthToken)의 일부로 저장됩니다.</p>
     *
     * @param memberId 회원 ID
     * @param authTokenId 기기별 세션 ID (JWT에 포함된 authTokenId)
     * @param fcmToken FCM 토큰 문자열 (Firebase SDK에서 생성)
     * @author Jaeik
     * @since 2.1.0
     */
    @Transactional
    public void registerFcmToken(Long memberId, Long authTokenId, String fcmToken) {

        if (fcmToken == null || fcmToken.isEmpty()) {
            throw new CustomException(ErrorCode.NOTIFICATION_NO_SEND_FCM_TOKEN);
        }

        AuthToken authToken = authTokenRepository.findById(authTokenId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_FCM_TOKEN_NOT_FOUND));

        // 보안 검증: AuthToken의 member_id와 요청한 memberId가 일치하는지 확인
        if (!authToken.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_AUTH_TOKEN);
        }

        authToken.updateFcmToken(fcmToken);
    }

    public AuthToken save(AuthToken authToken) {
        return authTokenRepository.save(authToken);
    }
}
