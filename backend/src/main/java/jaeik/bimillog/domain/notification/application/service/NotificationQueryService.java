package jaeik.bimillog.domain.notification.application.service;

import jaeik.bimillog.domain.notification.application.port.in.NotificationQueryUseCase;
import jaeik.bimillog.domain.notification.application.port.out.NotificationQueryPort;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * <h2>알림 조회 서비스</h2>
 * <p>
 * 헥사고날 아키텍처에서 NotificationQueryUseCase를 구현하는 Application Service입니다.
 * 사용자의 알림 목록 조회에 관한 비즈니스 로직을 처리하며, CQRS 패턴의 조회 부분을 담당합니다.
 * </p>
 * <p>
 * 읽기 전용 트랜잭션을 통해 조회 성능을 최적화하고, null 안전성을 보장하는 방어적 프로그래밍을 적용합니다.
 * 사용자 인증 정보 검증과 빈 목록 처리를 통해 안정적인 알림 조회 서비스를 제공합니다.
 * </p>
 * <p>NotificationQueryController에서 호출되며, NotificationQueryPort를 통해 데이터 저장소에 접근합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationQueryPort notificationQueryPort;

    /**
     * <h3>알림 목록 조회</h3>
     * <p>현재 로그인한 사용자의 모든 알림을 최신순으로 조회하여 반환합니다.</p>
     * <p>사용자 인증 정보가 유효하지 않은 경우 빈 목록을 반환하여 예외 발생을 방지합니다.</p>
     * <p>읽기 전용 트랜잭션으로 실행되어 조회 성능을 최적화하며, null 안전성을 보장합니다.</p>
     * <p>NotificationQueryController에서 사용자의 알림함 조회 API 요청을 처리하기 위해 호출됩니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (인증 컨텍스트)
     * @return 알림 엔티티 목록 (최신순 정렬, null 안전성 보장 - 빈 리스트 반환)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationList(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return Collections.emptyList();
        }
        
        List<Notification> notifications = notificationQueryPort.getNotificationList(userDetails.getUserId());
        
        return notifications != null ? notifications : Collections.emptyList();
    }
}