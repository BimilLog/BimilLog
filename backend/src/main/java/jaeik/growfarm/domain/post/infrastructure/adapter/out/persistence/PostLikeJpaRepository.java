package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.post.domain.PostLike;
import jaeik.growfarm.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;



public interface PostLikeJpaRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserAndPost(User user, Post post);
    void deleteByUserAndPost(User user, Post post);
    long countByPost(Post post);
}

