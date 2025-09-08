package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.user;

import jaeik.bimillog.domain.comment.application.port.out.CommentToUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 어댑터</h2>
 * <p>Comment 도메인에서 User 도메인의 In-Port를 통해 접근하는 어댑터</p>
 * <p>헥사고날 아키텍처를 준수하여 요구사항을 통한 도메인간 통신</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component("commentUserAdapter")
@RequiredArgsConstructor
public class CommentToUserAdapter implements CommentToUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>익명 댓글 지원을 위해 Optional 반환하며, 호출자가 필요에 따라 예외 처리를 결정할 수 있습니다.</p>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long userId) {
        return userQueryUseCase.findById(userId);
    }
}