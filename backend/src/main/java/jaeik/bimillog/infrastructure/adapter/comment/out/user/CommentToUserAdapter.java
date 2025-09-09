package jaeik.bimillog.infrastructure.adapter.comment.out.user;

import jaeik.bimillog.domain.comment.application.port.out.CommentToUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>Comment-User 도메인 간 통신 어댑터</h2>
 * <p>
 * Comment 도메인에서 User 도메인에 접근하기 위한 아웃바운드 어댑터입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 도메인 간 분리를 유지하면서 Comment 도메인이 
 * User 도메인의 데이터에 접근할 수 있도록 중개 역할을 수행합니다.
 * </p>
 * <p>
 * CommentService에서 댓글 작성자의 정보를 조회하거나 권한을 확인할 때 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component("commentUserAdapter")
@RequiredArgsConstructor
public class CommentToUserAdapter implements CommentToUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>User 도메인의 데이터를 조회합니다.</p>
     * <p>Comment 도메인에서 댓글 작성자의 정보를 확인하거나 권한 검증 시 사용됩니다.</p>
     * <p>익명 댓글 지원을 위해 Optional 반환하며, 호출자가 필요에 따라 예외 처리를 결정할 수 있습니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return Optional&lt;User&gt; 조회된 사용자 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long userId) {
        return userQueryUseCase.findById(userId);
    }
}