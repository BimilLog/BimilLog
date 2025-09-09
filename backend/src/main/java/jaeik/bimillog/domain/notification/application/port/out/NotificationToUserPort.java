package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>
 * 헥사고날 아키텍처에서 Notification 도메인이 User 도메인의 데이터에 접근하기 위한 Secondary Port입니다.
 * 도메인 간 의존성을 역전시켜 알림 생성 시 필요한 사용자 정보를 조회하는 외부 어댑터 인터페이스를 제공합니다.
 * </p>
 * <p>
 * 알림 생성 시 알림을 받을 사용자의 존재성 검증과 사용자 엔티티 참조 생성을 위해 사용됩니다.
 * User 도메인의 예외 처리를 위임하여 Notification 서비스는 순수한 User 엔티티를 받습니다.
 * </p>
 * <p>NotificationCommandService에서 사용되며, NotificationToUserAdapter에 의해 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationToUserPort {
    
    /**
     * <h3>사용자 ID로 조회</h3>
     * <p>알림을 받을 사용자의 정보를 사용자 ID로 조회하여 반환합니다.</p>
     * <p>존재하지 않는 사용자 ID인 경우 User 도메인의 예외를 발생시키며, Notification 도메인에서는 이를 그대로 전파합니다.</p>
     * <p>조회된 User 엔티티는 알림 생성 시 연관관계 설정에 사용되며, 사용자 정보 참조를 위해 활용됩니다.</p>
     * <p>NotificationCommandService에서 알림 생성 전 사용자 존재성 검증과 엔티티 참조 생성을 위해 호출됩니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return User 조회된 사용자 엔티티 (알림 연관관계용)
     * @author Jaeik
     * @since 2.0.0
     */
    User findById(Long userId);
}