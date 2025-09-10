package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.post.entity.Post;

/**
 * <h2>댓글-게시글 연동 포트</h2>
 * <p>댓글 도메인에서 게시글 도메인의 데이터에 접근하기 위한 포트입니다.</p>
 * <p>게시글 존재성 검증, 게시글 정보 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentToPostPort {

    /**
     * <h3>게시글 ID로 게시글 엔티티 조회</h3>
     * <p>특정 ID에 해당하는 게시글 엔티티를 조회합니다.</p>
     * <p>{@link CommentCommandService}에서 댓글 작성 시 게시글 유효성 검증 및 알림 발송을 위해 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @return Post 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long postId);
}
