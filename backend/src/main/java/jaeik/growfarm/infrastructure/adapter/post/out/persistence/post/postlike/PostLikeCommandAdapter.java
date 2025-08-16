package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.postlike;

import jaeik.growfarm.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 추천 커맨드 영속성 어댑터</h2>
 * <p>게시글 추천의 생성 및 삭제와 관련된 데이터베이스 작업을 처리합니다.</p>
 * <p>PostLikeCommandPort 인터페이스를 구현하여 게시글 추천 명령 처리 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostLikeCommandAdapter implements PostLikeCommandPort {
    private final PostLikeJpaRepository postLikeJpaRepository;

    /**
     * <h3>게시글 추천 저장</h3>
     * <p>새로운 게시글 추천 엔티티를 저장합니다.</p>
     *
     * @param postLike 저장할 PostLike 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(PostLike postLike) {
        postLikeJpaRepository.save(postLike);
    }

    /**
     * <h3>사용자와 게시글로 추천 삭제</h3>
     * <p>특정 사용자 및 게시글에 해당하는 추천를 삭제합니다.</p>
     *
     * @param user 삭제할 추천의 사용자 엔티티
     * @param post 삭제할 추천의 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByUserAndPost(User user, Post post) {
        postLikeJpaRepository.deleteByUserAndPost(user, post);
    }

    /**
     * <h3>게시글 ID로 모든 추천 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 추천 기록을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByPostId(Long postId) {
        postLikeJpaRepository.deleteAllByPostId(postId);
    }
}
