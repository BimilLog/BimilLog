package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.domain.Post;

/**
 * <h2>DeletePostPort</h2>
 * <p>
 *     게시글 데이터를 삭제하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface DeletePostPort {

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     *     게시글 엔티티를 데이터베이스에서 삭제합니다.
     * </p>
     * @param post 삭제할 게시글 엔티티
     */
    void delete(Post post);
}

