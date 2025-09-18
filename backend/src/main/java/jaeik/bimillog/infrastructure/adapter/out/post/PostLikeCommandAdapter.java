package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 추천 명령 어댑터</h2>
 * <p>게시글 추천 명령 포트의 JPA 구현체입니다.</p>
 * <p>게시글 추천 생성, 삭제 작업</p>
 * <p>JPA Repository를 활용한 데이터베이스 작업</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostLikeCommandAdapter implements PostLikeCommandPort {
    private final PostLikeRepository postLikeRepository;

    /**
     * <h3>게시글 추천 저장</h3>
     * <p>새로운 게시글 추천 엔티티를 JPA로 저장합니다.</p>
     * <p>PostLikeCommandUseCase가 게시글 추천 처리 시 호출합니다.</p>
     *
     * @param postLike 저장할 PostLike 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(PostLike postLike) {
        postLikeRepository.save(postLike);
    }

    /**
     * <h3>사용자와 게시글로 추천 삭제</h3>
     * <p>특정 사용자 및 게시글에 해당하는 추천을 JPA로 삭제합니다.</p>
     * <p>PostLikeCommandUseCase가 게시글 추천 취소 처리 시 호출합니다.</p>
     *
     * @param user 삭제할 추천의 사용자 엔티티
     * @param post 삭제할 추천의 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByUserAndPost(User user, Post post) {
        postLikeRepository.deleteByUserAndPost(user, post);
    }

    /**
     * <h3>게시글 ID로 모든 추천 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 추천 기록을 JPA로 삭제합니다.</p>
     * <p>PostDeleteUseCase가 게시글 삭제 시 관련 추천 데이터 정리를 위해 호출합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByPostId(Long postId) {
        postLikeRepository.deleteAllByPostId(postId);
    }
}
