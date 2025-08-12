package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.Post;

/**
 * <h2>CountPostLikePort</h2>
 * <p>
 *     게시글 좋아요 개수를 조회하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface CountPostLikePort {
    
    /**
     * <h3>게시글의 좋아요 개수 조회</h3>
     * <p>
     *     특정 게시글의 좋아요 개수를 조회합니다.
     * </p>
     * @param post 좋아요 개수를 조회할 게시글
     * @return 좋아요 개수
     */
    long countByPost(Post post);
}