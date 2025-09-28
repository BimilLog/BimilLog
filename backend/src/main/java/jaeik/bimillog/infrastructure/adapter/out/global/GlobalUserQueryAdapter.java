package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 조회 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 사용자 조회 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalUserQueryPort를 구현하여 도메인 간 사용자 조회 기능을 통합 제공합니다.</p>
 * <p>User 도메인의 UserQueryUseCase를 통해 실제 사용자 데이터에 접근합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalUserQueryAdapter implements GlobalUserQueryPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>User 도메인의 UserQueryUseCase를 통해 사용자 정보를 조회합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return Optional&lt;User&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long userId) {
        return userQueryUseCase.findById(userId);
    }

    /**
     * <h3>사용자명으로 사용자 조회</h3>
     * <p>특정 사용자명에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>User 도메인의 UserQueryUseCase를 통해 사용자 정보를 조회합니다.</p>
     *
     * @param userName 조회할 사용자명
     * @return Optional&lt;User&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByUserName(String userName) {
        return userQueryUseCase.findByUserName(userName);
    }

    /**
     * <h3>사용자명 존재 여부 확인</h3>
     * <p>특정 사용자명을 가진 사용자가 시스템에 존재하는지 확인합니다.</p>
     * <p>User 도메인의 UserQueryUseCase를 통해 존재 여부를 확인합니다.</p>
     *
     * @param userName 확인할 사용자명
     * @return boolean 사용자명이 존재하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userQueryUseCase.existsByUserName(userName);
    }

    /**
     * <h3>사용자 ID로 JPA 프록시 참조 조회</h3>
     * <p>실제 데이터베이스 조회 없이 사용자 ID를 가진 User 프록시 객체를 반환합니다.</p>
     * <p>User 도메인의 UserQueryUseCase를 통해 프록시 객체를 생성합니다.</p>
     *
     * @param userId 참조할 사용자 ID
     * @return User 프록시 객체 (실제 데이터는 지연 로딩)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User getReferenceById(Long userId) {
        return userQueryUseCase.getReferenceById(userId);
    }
}