package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence.post.postlike;

import jaeik.growfarm.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 추천 쿼리 영속성 어댑터</h2>
 * <p>게시글 추천의 조회와 관련된 데이터베이스 작업을 처리합니다.</p>
 * <p>PostLikeQueryPort 인터페이스를 구현하여 게시글 추천 조회 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostLikeQueryAdapter implements PostLikeQueryPort {
    private final PostLikeJpaRepository postLikeJpaRepository;

    /**
     * <h3>사용자와 게시글로 추천 존재 여부 확인</h3>
     * <p>특정 사용자 및 게시글에 추천가 존재하는지 확인합니다.</p>
     *
     * @param user 사용자 엔티티
     * @param post 게시글 엔티티
     * @return 추천가 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserAndPost(User user, Post post) {
        return postLikeJpaRepository.existsByUserAndPost(user, post);
    }

    /**
     * <h3>게시글 추천 수 조회</h3>
     * <p>특정 게시글의 추천 총 개수를 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return 해당 게시글의 추천 개수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public long countByPost(Post post) {
        return postLikeJpaRepository.countByPost(post);
    }
}
