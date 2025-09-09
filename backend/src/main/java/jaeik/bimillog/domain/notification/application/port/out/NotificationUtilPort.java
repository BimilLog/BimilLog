package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;

import java.util.List;

/**
 * <h2>알림 유틸리티 포트</h2>
 * <p>
 * 헥사고날 아키텍처에서 알림 수신 자격 검증과 FCM 토큰 조회를 정의하는 Secondary Port입니다.
 * 알림 발송 전 사용자의 수신 가능 여부 확인과 관련 토큰 조회에 대한 외부 어댑터 인터페이스를 제공합니다.
 * </p>
 * <p>
 * 사용자별 알림 설정, 차단 상태, 토큰 유효성 등을 종합적으로 검증하여 불필요한 알림 발송을 방지합니다.
 * SSE와 FCM 각각의 발송 조건을 별도로 검증하여 효율적인 알림 시스템을 지원합니다.
 * </p>
 * <p>NotificationSseService와 NotificationFcmService에서 사용되며, NotificationUtilAdapter에 의해 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationUtilPort {

    /**
     * <h3>SSE 알림 수신 자격 확인</h3>
     * <p>사용자가 특정 유형의 SSE 실시간 알림을 수신할 수 있는지 검증합니다.</p>
     * <p>사용자 알림 설정, 차단 상태, 비활성 상태 등을 종합적으로 검사하여 수신 가능 여부를 결정합니다.</p>
     * <p>NotificationSseService에서 SSE 알림 발송 전 검증을 위해 호출됩니다.</p>
     *
     * @param userId 검증할 사용자 ID
     * @param type   알림 유형 (COMMENT, PAPER_PLANT, POST_FEATURED)
     * @return SSE 알림 수신 가능 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean SseEligibleForNotification(Long userId, NotificationType type);

    /**
     * <h3>FCM 알림 수신 자격 검증 및 토큰 조회</h3>
     * <p>사용자가 특정 유형의 FCM 푸시 알림을 수신할 수 있는지 검증하고, 수신 가능한 경우 모든 FCM 토큰을 반환합니다.</p>
     * <p>알림 설정, 사용자 상태, 토큰 유효성 등을 종합적으로 검사하여 유효한 FCM 토큰만을 필터링합니다.</p>
     * <p>NotificationFcmService에서 FCM 알림 발송 전 토큰 조회를 위해 호출됩니다.</p>
     *
     * @param userId 검증할 사용자 ID
     * @param type   알림 유형 (COMMENT, PAPER_PLANT, POST_FEATURED)
     * @return 알림 수신 가능한 경우 유효한 FCM 토큰 목록, 불가능한 경우 빈 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<FcmToken> FcmEligibleFcmTokens(Long userId, NotificationType type);
}