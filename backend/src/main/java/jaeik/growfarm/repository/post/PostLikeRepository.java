package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
 * 게시글 추천 Repository
 * 게시글 좋아요 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    int countByPostId(Long postId);

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteAllByPostId(Long postId);
}
