package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.out.AuthTokenRepository;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>전역 FCM 토큰 저장 서비스</h2>
 * <p>여러 도메인에서 공통으로 사용하는 FCM 토큰 등록 기능을 제공하는 서비스입니다.</p>
 * <p>FCM 토큰은 AuthToken 테이블에 기기별 세션 정보로 저장됩니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmSaveService {
    private final AuthTokenRepository authTokenRepository;
    private final GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;

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
        globalAuthTokenSaveAdapter.save(authToken);
    }
}
