package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.board.Post;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {

    Page<Post> findAll(Pageable pageable);

    Page<Post> findByTitleContaining(@NotNull String query, Pageable pageable);

    Page<Post> findByTitleContainingOrContentContaining(@NotNull String titleQuery, @NotNull String contentQuery,
            Pageable pageable);

    Page<Post> findByUser_farmNameContaining(@NotNull String query, Pageable pageable);

    List<Post> findFeaturedPosts();

    List<Post> findByIsFeaturedIsTrue();

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE post SET is_featured = false")
    void resetFeaturedPosts();

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE post SET is_Featured = true WHERE post_id IN :postIds")
    void updateFeaturedStatus(@Param("postIds") List<Long> list);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE post SET views = views + 1 WHERE post_id = :postId")
    void incrementViews(@Param("postId") Long postId);

    // 해당 유저의 작성 글 목록 반환
    Page<Post> findByUserId(Long userId, Pageable pageable);

    // 해당 유저가 추천 누른 글 목록 반환
    Page<Post> findByLikedPosts(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM post_like WHERE post_id IN (SELECT post_id FROM post WHERE user_id = :userId)")
    void deletePostLikesByPostUserIds(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM post_like WHERE user_id = :userId")
    void deletePostLikesByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM post WHERE user_id = :userId")
    void deletePostsByUserId(@Param("userId") Long userId);
}
