package jaeik.growfarm.repository.post;

import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>게시글 커스텀 저장소</h2>
 * <p>커스텀 쿼리를 정의하는 인터페이스</p>
 * @author jaeik
 * @version 1.0
 */
public interface PostCustomRepository {

    void updateRealtimePopularPosts();

    List<Post> updateWeeklyPopularPosts();

    List<Post> updateHallOfFamePosts();

    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

    Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable);

}
