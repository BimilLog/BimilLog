package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/*
 * 커스텀 게시글 Repository
 * 게시글 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-05
 */
public interface PostCustomRepository {

    List<Post> updateRealtimePopularPosts();

    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

}
