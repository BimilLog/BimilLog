package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;

import java.util.Optional;

/**
 * <h2>게시글 조회 포트</h2>
 * <p>Comment 도메인에서 Post 도메인의 데이터에 접근하기 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentToPostPort {

    /**
     * <h3>게시글ID로 게시글 조회</h3>
     * <p>게시글 ID로 게시글을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Optional<Post> 조회된 게시글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Post> findById(Long postId);
}
