package jaeik.bimillog.domain.user.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>사용자 명령 포트</h2>
 * <p>사용자 정보 생성/수정을 위한 출력 포트</p>
 * <p>CQRS 패턴에 따라 명령 전용 포트로 분리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserCommandPort {
    
    /**
     * <h3>사용자 정보 저장</h3>
     * <p>사용자 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    User save(User user);
}
