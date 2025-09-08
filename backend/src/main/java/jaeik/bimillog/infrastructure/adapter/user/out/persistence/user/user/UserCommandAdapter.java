package jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user;

import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>사용자 명령 어댑터</h2>
 * <p>사용자 정보 생성/수정을 위한 영속성 어댑터</p>
 * <p>CQRS 패턴에 따라 명령 전용 어댑터로 분리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

    private final UserRepository userRepository;

    /**
     * <h3>사용자 정보 저장</h3>
     * <p>사용자 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @throws IllegalArgumentException 사용자가 null인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return userRepository.save(user);
    }
}