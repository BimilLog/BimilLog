package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.application.service.FcmService;
import jaeik.bimillog.domain.notification.application.service.SseService;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;

import java.util.List;

/**
 * <h2>알림 유틸리티 포트</h2>
 * <p>알림 수신 자격 검증과 FCM 토큰 조회를 담당하는 포트입니다.</p>
 * <p>사용자별 알림 설정, 차단 상태, 토큰 유효성 검증</p>
 * <p>SSE와 FCM 각각의 발송 조건 별도 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationUtilPort {

    /**
     * <h3>SSE 알림 수신 자격 확인</h3>
     * <p>사용자가 특정 유형의 SSE 실시간 알림을 수신할 수 있는지 검증합니다.</p>
     * <p>사용자 알림 설정, 차단 상태, 비활성 상태 등을 종합적으로 검사하여 수신 가능 여부를 결정합니다.</p>
     * <p>{@link SseService}에서 SSE 알림 발송 전 검증을 위해 호출됩니다.</p>
     *
     * @param memberId 검증할 사용자 ID
     * @param type   알림 유형 (COMMENT, PAPER_PLANT, POST_FEATURED)
     * @return SSE 알림 수신 가능 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean SseEligibleForNotification(Long memberId, NotificationType type);

    /**
     * <h3>FCM 알림 수신 자격 검증 및 토큰 조회</h3>
     * <p>사용자가 특정 유형의 FCM 푸시 알림을 수신할 수 있는지 검증하고, 수신 가능한 경우 모든 FCM 토큰을 반환합니다.</p>
     * <p>알림 설정, 사용자 상태, 토큰 유효성 등을 종합적으로 검사하여 유효한 FCM 토큰만을 필터링합니다.</p>
     * <p>{@link FcmService}에서 FCM 알림 발송 전 토큰 조회를 위해 호출됩니다.</p>
     *
     * @param memberId 검증할 사용자 ID
     * @param type   알림 유형 (COMMENT, PAPER_PLANT, POST_FEATURED)
     * @return 알림 수신 가능한 경우 유효한 FCM 토큰 목록, 불가능한 경우 빈 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<FcmToken> FcmEligibleFcmTokens(Long memberId, NotificationType type);
}