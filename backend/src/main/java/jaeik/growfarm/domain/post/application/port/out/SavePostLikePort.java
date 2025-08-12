package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.PostLike;

/**
 * <h2>SavePostLikePort</h2>
 * <p>
 *     게시글 좋아요 데이터를 저장하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface SavePostLikePort {

    /**
     * <h3>게시글 좋아요 저장</h3>
     * <p>
     *     게시글 좋아요 엔티티를 데이터베이스에 저장합니다.
     * </p>
     * @param postLike 저장할 게시글 좋아요 엔티티
     */
    void save(PostLike postLike);
}

