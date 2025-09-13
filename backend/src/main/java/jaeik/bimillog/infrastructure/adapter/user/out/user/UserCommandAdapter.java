package jaeik.bimillog.infrastructure.adapter.user.out.user;

import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.SaveUserAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 어댑터</h2>
 * <p>사용자 정보 생성/수정을 위한 영속성 어댑터</p>
 * <p>사용자 엔티티 저장 및 업데이트</p>
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
     * <p>{@link SaveUserAdapter}에서 신규 사용자 생성 시 호출됩니다.</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @throws IllegalArgumentException 사용자가 null인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User는 null이 될 수 없습니다.");
        }
        return userRepository.save(user);
    }
}