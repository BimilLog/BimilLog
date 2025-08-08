package jaeik.growfarm.service.post;

import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>게시글 상호작용 서비스</h2>
 * <p>
 * 사용자의 게시글 상호작용(좋아요, 조회수)을 담당하는 서비스
 * 트랜잭션 경계를 최소화하여 성능을 최적화한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
public class PostInteractionService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    /**
     * <h3>게시글 좋아요/취소 토글</h3>
     * <p>
     * 좋아요가 이미 존재하면 취소하고, 없으면 추가한다.
     * </p>
     *
     * @param existingLike 기존 좋아요 엔티티 (Optional)
     * @param post         대상 게시글 엔티티
     * @param user         사용자 엔티티
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional
    public void toggleLike(Optional<PostLike> existingLike, Post post, Users user) {
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
        } else {
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(postLike);
        }
    }

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>
     * 게시글의 조회수를 1 증가시킨다.
     * </p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional
    public void incrementViewCount(Long postId) {
        postRepository.incrementViews(postId);
    }
}