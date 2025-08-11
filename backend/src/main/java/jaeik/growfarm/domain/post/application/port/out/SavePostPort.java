package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.domain.Post;

/**
 * <h2>SavePostPort</h2>
 * <p>
 *     게시글 데이터를 저장하고 수정하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface SavePostPort {

    /**
     * <h3>게시글 저장</h3>
     * <p>
     *     게시글 엔티티를 데이터베이스에 저장합니다.
     * </p>
     * @param post 저장할 게시글 엔티티
     * @return 저장된 게시글 엔티티
     */
    Post save(Post post);
}

