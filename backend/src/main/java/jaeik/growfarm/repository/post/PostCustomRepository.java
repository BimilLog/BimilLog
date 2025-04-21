package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostCustomRepository {

    List<Post> findFeaturedPosts();

    Page<Post> findByLikedPosts(Long userId, Pageable pageable);


}
