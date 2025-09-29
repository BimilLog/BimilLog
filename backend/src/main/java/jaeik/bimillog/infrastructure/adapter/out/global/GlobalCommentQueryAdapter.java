package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCommentQueryPort;
import jaeik.bimillog.infrastructure.adapter.out.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>전역 댓글 조회 어댑터</h2>
 * <p>도메인 경계를 넘어 댓글 엔티티를 조회하는 전역 포트 구현체입니다.</p>
 * <p>여러 도메인에서 공통으로 필요한 댓글 조회 기능을 제공합니다.</p>
 * <p>핵사고날 아키텍처에서 도메인 간 직접 참조를 방지하면서 필요한 데이터 접근을 가능하게 합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class GlobalCommentQueryAdapter implements GlobalCommentQueryPort {

    private final CommentRepository commentRepository;


    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>댓글 ID로 댓글을 조회합니다.</p>
     * <p>존재하지 않는 댓글 ID인 경우 예외를 발생시킵니다.</p>
     * <p>CommentCommandService, CommentQueryService, AdminCommandService 등
     * 여러 도메인 서비스에서 댓글 엔티티 조회 시 호출됩니다.</p>
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
