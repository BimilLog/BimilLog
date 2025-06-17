package jaeik.growfarm.service.post;

import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글글 업데이트 서비스</h2>
 * <p>
 * 게시글의 DB작업을 처리하는 서비스
 * </p>
 *
 *
 * @author Jaeik
 * @version  1.0.0
 */
@Service
@RequiredArgsConstructor
public class PostUpdateService {
    private final PostRepository postRepository;

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글을 삭제한다.</p>
     *
     * @param post 게시글 엔티티
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void postDelete(Post post) {
        postRepository.delete(post);
    }
}
