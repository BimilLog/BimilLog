package jaeik.bimillog.infrastructure.adapter.paper.out.persistence.user;

import jaeik.bimillog.domain.paper.application.port.out.LoadUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>
 * Secondary Adapter: Paper 도메인에서 User 도메인의 UseCase를 통해 접근하는 어댑터
 * 도메인 간 통신에서 헥사고날 아키텍처 원칙을 준수하여 UseCase 인터페이스만 사용
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * {@inheritDoc}
     * 
     * <p>User 도메인의 UserQueryUseCase를 위임하여 사용자를 조회합니다.</p>
     */
    @Override
    public Optional<User> findByUserName(String userName) {
        return userQueryUseCase.findByUserName(userName);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>User 도메인의 UserQueryUseCase를 위임하여 사용자 존재 여부를 확인합니다.</p>
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userQueryUseCase.existsByUserName(userName);
    }
}