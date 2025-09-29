package jaeik.bimillog.infrastructure.adapter.out.comment;

import jaeik.bimillog.domain.comment.application.service.CommentQueryService;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCommentQueryPort;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalCommentQueryAdapter implements GlobalCommentQueryPort {

    private final CommentRepository commentRepository;


    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     * <p>존재하지 않는 댓글 ID인 경우 예외를 발생시킵니다.</p>
     * <p>{@link CommentQueryService}에서 댓글 ID로 댓글 조회 시 호출됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CommentCustomException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
