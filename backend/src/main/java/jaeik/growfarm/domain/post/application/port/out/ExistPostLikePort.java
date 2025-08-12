package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.post.entity.Post;

/**
 * <h2>ExistPostLikePort</h2>
 * <p>
 *     게시글 좋아요 데이터 존재 여부를 확인하는 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
public interface ExistPostLikePort {

    /**
     * <h3>게시글 좋아요 존재 여부 확인</h3>
     * <p>
     *     사용자와 게시글을 기준으로 좋아요 데이터가 존재하는지 확인합니다.
     * </p>
     * @param user 사용자
     * @param post 게시글
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByUserAndPost(User user, Post post);
}

