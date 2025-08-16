package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>Auth 도메인에서 User 도메인 정보 조회를 위한 아웃바운드 어댑터</p>
 * <p>헥사고날 아키텍처: LoadUserPort(out포트) -> LoadUserAdapter(out어댑터) -> UserQueryUseCase(User의 in포트)</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
public class LoadUserAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>User 도메인의 UserQueryUseCase를 통해 사용자 정보를 조회합니다.</p>
     * <p>헥사고날 아키텍처 원칙에 따라 Auth 서비스는 out포트를 의존하고, out어댑터에서 다른 도메인의 유스케이스를 의존합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long id) {
        return userQueryUseCase.findById(id);
    }
}