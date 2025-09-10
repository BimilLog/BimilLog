package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.application.service.NotificationCommandService;
import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>알림 도메인이 사용자 정보에 접근하기 위한 포트입니다.</p>
 * <p>알림 생성 시 사용자 존재성 검증, 사용자 엔티티 참조 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationToUserPort {
    
    /**
     * <h3>사용자 ID로 조회</h3>
     * <p>알림을 받을 사용자의 정보를 사용자 ID로 조회하여 반환합니다.</p>
     * <p>존재하지 않는 사용자 ID인 경우 User 도메인의 예외를 발생시킵니다.</p>
     * <p>조회된 User 엔티티는 알림 생성 시 연관관계 설정에 사용됩니다.</p>
     * <p>{@link NotificationCommandService}에서 알림 생성 전 사용자 존재성 검증과 엔티티 참조 생성을 위해 호출됩니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    User findById(Long userId);
}