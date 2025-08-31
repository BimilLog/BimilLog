package jaeik.bimillog.infrastructure.adapter.post.out.persistence.user;

import jaeik.bimillog.domain.post.application.port.out.LoadUserInfoPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>사용자 로드 아웃 어댑터</h2>
 * <p>Post 도메인에서 사용자 정보를 로드하기 위한 아웃고잉 어댑터입니다.</p>
 * <p>User 도메인의 인-포트(UserQueryUseCase)를 통해 사용자 정보를 조회합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class LoadUserInfoAdapter implements LoadUserInfoPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 성능 최적화를 위해 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 프록시 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User getReferenceById(Long userId) {
        return userQueryUseCase.getReferenceById(userId);
    }
}
