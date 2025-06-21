package jaeik.growfarm.service.post;

import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>게시글 업데이트 서비스</h2>
 * <p>
 * 게시글의 DB작업을 처리하는 서비스
 * </p>
 *
 *
 *
 * @author Jaeik
 * @version  1.0.0
 */
@Service
@RequiredArgsConstructor
public class PostUpdateService {
    private final PostRepository postRepository;
    private final CommentClosureRepository commentClosureRepository;
    private final PostLikeRepository postLikeRepository;

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글을 삭제한다.</p>
     *
     * @param post 게시글 엔티티
     * @param commentIds 삭제할 댓글 ID 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void postDelete(Post post, List<Long> commentIds) {
        if (!commentIds.isEmpty()) {
            commentClosureRepository.deleteByCommentIds(commentIds);
        }
        postRepository.delete(post);
    }

    /**
     * <h3>글 추천 / 추천 취소</h3>
     * <p>글 추천 테이블에 엔티티가 이미 존재할 때는 추천 취소를 한다.</p>
     * <p>존재하지 않을 때는 추천을 추가한다.</p>
     *
     * @param existingLike Optional<PostLike> 댓글 추천 엔티티
     * @param post         글 엔티티
     * @param user         추천을 누른 사용자 엔티티
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void savePostLike(Optional<PostLike> existingLike, Post post, Users user) {
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
     * <p>게시글의 조회수를 1 증가시킨다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void updateViewCount(Long postId) {
        postRepository.incrementViews(postId);
    }
}
