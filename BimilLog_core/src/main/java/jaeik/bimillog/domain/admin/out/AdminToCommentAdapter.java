package jaeik.bimillog.domain.admin.out;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.repository.CommentRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>관리자 댓글 조회 어댑터</h2>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class AdminToCommentAdapter {
    private final CommentRepository commentRepository;

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    // CommentIds로 댓글 리스트 조회
    public List<Comment> findAllByIds(List<Long> commentIds) {
        return commentRepository.findAllByIdsWithMember(commentIds);
    }
}
