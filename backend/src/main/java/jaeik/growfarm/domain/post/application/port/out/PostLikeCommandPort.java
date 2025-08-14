package jaeik.growfarm.domain.post.application.port.out;

import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>추천 관리 포트</h2>
 * <p>
 *     게시글 추천 데이터의 생성/삭제를 담당하는 통합 Port 인터페이스입니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public interface PostLikeCommandPort {

    /**
     * <h3>게시글 추천 저장</h3>
     * <p>
     *     게시글 추천 엔티티를 데이터베이스에 저장합니다.
     * </p>
     * @param postLike 저장할 게시글 추천 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    void save(PostLike postLike);

    /**
     * <h3>게시글 추천 삭제</h3>
     * <p>
     *     사용자와 게시글을 기준으로 추천 데이터를 삭제합니다.
     * </p>
     * @param user 사용자
     * @param post 게시글
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteByUserAndPost(User user, Post post);

    /**
     * <h3>게시글 ID로 모든 추천 삭제</h3>
     * <p>특정 게시글에 대한 모든 추천 데이터를 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);
}