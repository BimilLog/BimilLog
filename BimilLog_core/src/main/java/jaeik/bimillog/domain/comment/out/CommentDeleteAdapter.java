package jaeik.bimillog.domain.comment.out;

import jaeik.bimillog.domain.comment.application.port.out.CommentDeletePort;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
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
public class CommentDeleteAdapter implements CommentDeletePort {

    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글의 하드 삭제를 수행합니다.</p>
     * <p>클로저 테이블과 댓글 엔티티를 함께 완전 제거합니다.</p>
     * <p>{@link CommentCommandService}에서 자손이 없는 댓글 삭제 시 호출됩니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteComment(Long commentId) {
        commentRepository.deleteClosuresByDescendantId(commentId);
        commentRepository.hardDeleteComment(commentId);
    }

}