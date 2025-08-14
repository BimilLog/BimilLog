package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>글 추천 조회 포트</h2>
 * <p>
 *     게시글 추천 조회를 담당하는 통합 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface PostLikeQueryPort {

    /**
     * <h3>게시글 추천 존재 여부 확인</h3>
     * <p>
     *     사용자와 게시글을 기준으로 추천 데이터가 존재하는지 확인합니다.
     * </p>
     * @param user 사용자
     * @param post 게시글
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByUserAndPost(User user, Post post);

    /**
     * <h3>게시글의 추천 개수 조회</h3>
     * <p>
     *     특정 게시글의 추천 개수를 조회합니다.
     * </p>
     * @param post 추천 개수를 조회할 게시글
     * @return 추천 개수
     */
    long countByPost(Post post);
}