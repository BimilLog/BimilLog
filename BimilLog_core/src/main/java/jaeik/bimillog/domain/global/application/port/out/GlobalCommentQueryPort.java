package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.Comment;

public interface GlobalCommentQueryPort {

    /**
     * <h3>댓글 ID로 단일 댓글 조회</h3>
     * <p>특정 ID에 해당하는 댓글 엔티티를 조회합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 수정/삭제 권한 확인 시 호출됩니다.</p>
     *
     * @param id 댓글 ID
     * @return Comment 조회된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Comment findById(Long id);
}
