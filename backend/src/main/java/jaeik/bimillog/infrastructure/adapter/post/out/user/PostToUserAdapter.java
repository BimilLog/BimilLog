package jaeik.bimillog.infrastructure.adapter.post.out.user;

import jaeik.bimillog.domain.post.application.port.out.PostToUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>PostToUserAdapter</h2>
 * <p>
 * Post 도메인에서 User 도메인으로의 크로스 도메인 어댑터입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 도메인 간 의존성을 관리하고 도메인의 독립성을 보장하며,
 * PostToUserPort 인터페이스를 통해 Post 도메인이 필요로 하는 User 정보 조회 기능을 제공합니다.
 * </p>
 * <p>
 * PostCommandService에서 게시글 작성 시 사용자 참조 생성을 위해 호출되며,
 * JPA 프록시 객체를 활용하여 불필요한 쿼리 실행 없이 효율적인 연관 관계를 설정합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostToUserAdapter implements PostToUserPort {

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
