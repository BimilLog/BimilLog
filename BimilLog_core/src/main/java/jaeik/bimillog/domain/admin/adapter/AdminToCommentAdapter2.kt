package jaeik.bimillog.domain.admin.adapter

import jaeik.bimillog.domain.comment.entity.jpa.Comment
import jaeik.bimillog.domain.comment.repository.CommentRepository
import org.springframework.stereotype.Component

/**
 * <h2>관리자 댓글 조회 어댑터</h2>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
class AdminToCommentAdapter2(private val commentRepository: CommentRepository) {

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    fun findById(commentId: Long?) : Comment {
        return commentRepository.findById(commentId).orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND))
    }

    fun findAllByIds(commentIds: List<Long>) : List<Comment> {
        return commentRepository.findAllByIdsWithMember(commentIds);
    }
}