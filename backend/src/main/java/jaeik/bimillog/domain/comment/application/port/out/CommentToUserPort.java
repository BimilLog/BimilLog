package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>Comment 도메인에서 User 도메인의 데이터를 조회하기 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentToUserPort {

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>익명 댓글 지원을 위해 Optional 반환하며, 호출자가 필요에 따라 예외 처리를 결정할 수 있습니다.</p>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findById(Long userId);
}