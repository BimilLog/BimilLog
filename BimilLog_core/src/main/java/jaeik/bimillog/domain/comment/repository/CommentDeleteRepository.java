package jaeik.bimillog.domain.comment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>댓글 삭제 어댑터</h2>
 * <p>댓글 하드 삭제 포트의 구현체입니다.</p>
 * <p>자손이 없는 댓글의 완전 제거 처리</p>
 * <p>클로저 테이블과 댓글 엔티티 함께 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentDeleteRepository {

    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글의 하드 삭제를 수행합니다.</p>
     * <p>클로저 테이블과 댓글 엔티티를 함께 완전 제거합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteComment(Long commentId) {
        commentRepository.deleteClosuresByDescendantId(commentId);
        commentRepository.hardDeleteComment(commentId);
    }
}
