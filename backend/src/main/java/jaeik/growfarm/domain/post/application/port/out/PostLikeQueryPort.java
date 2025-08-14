package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>PostLikeQueryPort</h2>
 * <p>
 *     게시글 좋아요 데이터의 조회를 담당하는 통합 Port 인터페이스입니다.
 *     ISP(Interface Segregation Principle)를 따라 Query 관련 기능만 포함합니다.
 * </p>
 *
 * @author jaeik
 * @since 2.0.0
 * @version 2.0.0
 */
public interface PostLikeQueryPort {

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