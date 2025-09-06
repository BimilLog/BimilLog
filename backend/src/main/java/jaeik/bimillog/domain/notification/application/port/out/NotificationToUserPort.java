package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>Notification 도메인에서 User 도메인의 데이터를 조회하기 위한 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationToUserPort {
    
    /**
     * <h3>사용자 ID로 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 조회된 사용자 객체
     * @throws UserCustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    User findById(Long userId);
}