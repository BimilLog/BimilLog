package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.post.entity.Post;

/**
 * <h2>DeletePostLikePort</h2>
 * <p>
 *     게시글 좋아요 데이터를 삭제하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface DeletePostLikePort {

    /**
     * <h3>게시글 좋아요 삭제</h3>
     * <p>
     *     사용자와 게시글을 기준으로 좋아요 데이터를 삭제합니다.
     * </p>
     * @param user 사용자
     * @param post 게시글
     */
    void deleteByUserAndPost(User user, Post post);

    /**
     * <h3>게시글 ID로 모든 좋아요 삭제</h3>
     * <p>특정 게시글에 대한 모든 좋아요 데이터를 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);
}

